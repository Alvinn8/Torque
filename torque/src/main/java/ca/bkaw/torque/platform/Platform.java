package ca.bkaw.torque.platform;

import ca.bkaw.torque.TorqueCommand;
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
}
