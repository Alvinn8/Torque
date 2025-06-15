package ca.bkaw.torque.platform;

import org.joml.Matrix4f;

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
}
