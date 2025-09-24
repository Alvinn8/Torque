package ca.bkaw.torque.platform;

import ca.bkaw.torque.platform.entity.InteractionEntity;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

/**
 * A world/dimension in the game, such as the overworld, the nether or the end.
 */
public interface World {
    /**
     * Spawn an item display entity.
     *
     * @param position The position in the world to spawn the item display.
     * @return The item display entity.
     */
    @NotNull
    ItemDisplay spawnItemDisplay(@NotNull Vector3dc position);

    /**
     * Spawn an interaction entity.
     *
     * @param position The position in the world to spawn the interaction entity.
     * @return The interaction entity.
     */
    @NotNull
    InteractionEntity spawnInteractionEntity(@NotNull Vector3dc position);

    /**
     * Get the block state at the given position in the world.
     *
     * @param position The block coordinates.
     * @return The block state.
     */
    @NotNull
    BlockState getBlock(@NotNull Vector3ic position);

    void spawnParticle(Vector3ic blockPos, Identifier identifier);
}
