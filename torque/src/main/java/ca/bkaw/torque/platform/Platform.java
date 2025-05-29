package ca.bkaw.torque.platform;

import ca.bkaw.torque.TorqueCommand;
import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

/**
 * An abstraction over the platform that the game runs on.
 */
public interface Platform {
    /**
     * Register the command handler that the platform should invoke when
     * the {@code /torque} command is used.
     *
     * @param torqueCommand The command handler.
     */
    void setup(@NotNull TorqueCommand torqueCommand);

    /**
     * Create an item that renders as the specified model.
     *
     * @param modelIdentifier The identifier of the model to render.
     * @return The item stack.
     */
    ItemStack createModelItem(@NotNull Identifier modelIdentifier);

    /**
     * Inject a channel handler into the server networking pipeline.
     *
     * @param channelHandler The channel handler to inject.
     * @param handlerKey The key of the channel.
     */
    void injectChannelHandler(ChannelHandler channelHandler, Identifier handlerKey) throws ReflectiveOperationException;

    /**
     * Remove a channel handler from the server networking pipeline.
     * <p>
     * Existing connections will still have the channel handler.
     *
     * @param handlerKey The handler key.
     */
    void uninjectChannelHandler(Identifier handlerKey) throws ReflectiveOperationException;

    /**
     * Get the TCP port on which the server is listening to.
     *
     * @return The port.
     */
    int getPort();
}
