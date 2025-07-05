package ca.bkaw.torque.platform;

import org.joml.Vector3i;
import org.joml.Vector3ic;

public interface BlockState {
    /**
     * Check if this block is water, or a waterlogged block.
     *
     * @return Whether this block is waterlogged or not.
     */
    boolean isWaterlogged();

    boolean isCollidable(World world, Vector3ic position);
}
