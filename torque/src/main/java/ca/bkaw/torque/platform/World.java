package ca.bkaw.torque.platform;

import org.joml.Vector3dc;

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
    ItemDisplay spawnItemDisplay(Vector3dc position);
}
