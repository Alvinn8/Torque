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

    /**
     * Find the top surface of the first collidable block geometry at the given
     * horizontal position, scanning downward from {@code yTop} to {@code yBottom}.
     * <p>
     * The query is against the blocks' collision shapes at the exact horizontal
     * point, so slabs report 0.5 and stairs report 0.5 or 1.0 depending on which
     * half of the stair the point is over.
     * <p>
     * If geometry is already solid at {@code yTop}, the surface is clamped to
     * {@code yTop}. See
     * {@link ca.bkaw.torque.terrain.GroundHeightFunction#getGroundHeight} - this
     * method is the in-game implementation of that interface, usable as
     * {@code world::getGroundHeight}.
     *
     * @param x The x coordinate. Unit: meter.
     * @param z The z coordinate. Unit: meter.
     * @param yTop The top of the scan window. Unit: meter.
     * @param yBottom The bottom of the scan window. Unit: meter.
     * @return The surface y, clamped to at most {@code yTop}, or {@link Double#NaN}
     * if there is no surface within the window.
     */
    double getGroundHeight(double x, double z, double yTop, double yBottom);

    void spawnParticle(Vector3ic blockPos, Identifier identifier);
}
