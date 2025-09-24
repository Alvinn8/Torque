package ca.bkaw.torque.assets.send;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.TorqueAssets;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.entity.Player;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Sends resource packs by replying to HTTP requests received
 * on the server's TCP connections.
 */
@ChannelHandler.Sharable
public class BuiltInTcpResourcePackSender extends ChannelInboundHandlerAdapter implements ResourcePackSender {
    private static final Identifier HANDLER_KEY = new Identifier("torque", "torque_resource_pack_sender");
    private static final String PATH = "/torque/resource_pack.zip";

    private final Torque torque;

    public BuiltInTcpResourcePackSender(Torque torque) throws ReflectiveOperationException {
        this.torque = torque;
        this.torque.getPlatform().injectChannelHandler(this, HANDLER_KEY);
    }

    @Override
    public void send(@NotNull Player player, boolean required, @Nullable String prompt) {
        int port = this.torque.getPlatform().getPort();
        TorqueAssets assets = this.torque.getAssets();
        byte[] sha1Hash = assets.getSha1Hash();
        if (sha1Hash == null) {
            Torque.LOGGER.warning("Resource pack does not exist yet, but tried to send it.");
            return;
        }
        String url = "http://" + Utils.getHostnameFor(player) + ":" + port + PATH;
        player.sendResourcePack(TorqueAssets.PACK_UUID, url, sha1Hash, required, prompt);
    }

    @Override
    public void remove() throws ReflectiveOperationException {
        this.torque.getPlatform().uninjectChannelHandler(HANDLER_KEY);
    }

    @Override
    public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        byteBuf.markReaderIndex();
        if (!this.handle(ctx, byteBuf)) {
            // handle returned false, reset reader and call the super method to let vanilla
            // handle the connection.
            byteBuf.resetReaderIndex();
            super.channelRead(ctx, msg);
        }
    }

    private boolean handle(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        // There needs to be at least 14 bytes for "GET /torque/<resource pack id>" to fit.
        if (byteBuf.capacity() < 14) return false;

        // Start by efficiently comparing byte by byte as this is some hot networking
        // code. This code runs for every received packet.

        if (byteBuf.readByte() != 'G') return false;
        if (byteBuf.readByte() != 'E') return false;
        if (byteBuf.readByte() != 'T') return false;
        if (byteBuf.readByte() != ' ') return false;

        // An HTTP GET request was received.
        // Let's ensure it is requesting a resource pack from torque

        byte[] pathBytes = PATH.getBytes(StandardCharsets.UTF_8);
        for (byte pathByte : pathBytes) {
            if (byteBuf.readByte() != pathByte) return false;
        }

        // Read the pack id from the path

        StringBuilder resourcePackId = new StringBuilder();
        byte b;
        while (byteBuf.readableBytes() > 0 && (b = byteBuf.readByte()) != ' ') {
            resourcePackId.append((char) b);
        }

        // TODO we may want to create a request id and only reply to that to avoid people
        //  using resource packs as a way to slow the server / waste network bandwidth.
        //  Currently, we ignore the id.

        // Get the requested pack
        TorqueAssets assets = this.torque.getAssets();
        Path resourcePackPath = assets.getResourcePackPath();
        byte[] sha1Hash = assets.getSha1Hash();

        if (sha1Hash == null) {
            // Sorry, we do not gracefully reply with an HTTP 404 response. We just close
            // the connection.
            ctx.close();

            // Return true, we have handled the packet.
            return true;
        }

        try {
            long contentLength = Files.size(resourcePackPath);

            String headerText =
                """
                    HTTP/1.1 200 OK
                    Server: Torque
                    Content-Type: application/zip
                    Content-Length: %d
                    
                    """.formatted(contentLength);
            byte[] headerBytes = headerText.getBytes(StandardCharsets.UTF_8);

            ByteBuf response = Unpooled.buffer(headerBytes.length + (int) contentLength);

            response.writeBytes(headerBytes);

            ByteBufOutputStream stream = new ByteBufOutputStream(response);
            Files.copy(resourcePackPath, stream);
            stream.close();

            // Send the response
            ctx.pipeline().firstContext().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

            // Return true, we have handled the packet
            return true;
        } catch (IOException e) {
            Torque.LOGGER.log(Level.SEVERE, "Failed to reply with resource pack.", e);

            // Sorry, we do not gracefully reply with an HTTP 500 response. We just close
            // the connection.
            ctx.close();

            // Return true, we have handled the packet.
            return true;
        }
    }
}
