package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

public record FabricPlayer(ServerPlayer entity) implements Player {
    @Override
    public void sendResourcePack(UUID id, String url, byte[] hash, boolean required, @Nullable String prompt) {
        this.entity.connection.send(new ClientboundResourcePackPushPacket(
            id, url,
            HexFormat.of().formatHex(hash),
            required,
            Optional.ofNullable(prompt).map(Component::literal)
        ));
    }

    @Override
    public InetSocketAddress getAddress() {
        if (this.entity.connection.getRemoteAddress() instanceof InetSocketAddress address) {
            return address;
        }
        return null;
    }
}
