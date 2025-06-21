package ca.bkaw.torque.platform;

import org.joml.Matrix4f;
import org.joml.Vector3d;

/**
 * An item display entity.
 */
public interface ItemDisplay {
    /**
     * Set the item being displayed by this item display.
     *
     * @param item The item stack.
     */
    void setItem(ItemStack item);

    /**
     * Set the affine transform matrix.
     *
     * @param affineTransformMatrix The affine transform matrix.
     */
    void setTransformation(Matrix4f affineTransformMatrix);

    /**
     * Set the world position of the item display.
     *
     * @param position The coordinates in the world to set the position to.
     */
    void setPosition(Vector3d position);

    /**
     * Set the interpolation duration for teleportation.
     *
     * @param ticks The number of ticks to interpolate.
     */
    void setTeleportDuration(int ticks);

    /**
     * Set the interpolation duration for transformations.
     *
     * @param ticks The number of ticks to interpolate.
     */
    void setInterpolationDuration(int ticks);

    /**
     * Tell the client to start interpolating after the given number of ticks.
     *
     * @param ticks The number of ticks.
     */
    void setStartInterpolation(int ticks);

    /**
     * Remove the entity from the world.
     */
    void remove();
}
