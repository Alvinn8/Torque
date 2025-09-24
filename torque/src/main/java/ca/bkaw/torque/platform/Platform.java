package ca.bkaw.torque.platform;

import ca.bkaw.torque.PlatformEvents;
import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An abstraction over the platform that the game runs on.
 */
public interface Platform {
    /**
     * Register the command handler that the platform should invoke when
     * the {@code /torque} command is used.
     *
     * @param torqueCommand The command handler.
     * @param eventHandler The instance to invoke event methods on.
     */
    void setup(@NotNull TorqueCommand torqueCommand, @NotNull PlatformEvents eventHandler);

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

    /**
     * Run a runnable each server game tick.
     *
     * @param runnable The runnable to run each tick.
     */
    void runEachTick(@NotNull Runnable runnable);

    /**
     * Get all item display entities currently loaded in the server.
     *
     * @return The item display entities.
     */
    List<ItemDisplay> getAllItemDisplays();
}
