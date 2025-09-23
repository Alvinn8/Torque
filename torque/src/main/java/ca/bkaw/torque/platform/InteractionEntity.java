package ca.bkaw.torque.platform;

import org.joml.Vector3dc;

public interface InteractionEntity {

    /**
     * Set the world position of the item display.
     *
     * @param position The coordinates in the world to set the position to.
     */
    void setPosition(Vector3dc position);

    /**
     * Set the axis-aligned size of the interaction entity.
     *
     * @param width The width in the x and z directions.
     * @param height The height in the y direction.
     */
    void setSize(float width, float height);
}
