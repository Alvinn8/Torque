package ca.bkaw.torque.model;

import org.joml.Vector3f;

public class Seat {
    public static final double VERTICAL_OFFSET = 1.0;

    /**
     * The translation from the vehicle's center of mass to the position that the
     * display entity should be at to position a player as a passenger in this seat.
     * Measured local coordinates relative to the vehicle.
     */
    private final Vector3f translation;
    private final boolean driver;

    public Seat(Vector3f translation, boolean driver) {
        this.translation = translation;
        this.driver = driver;
    }

    public Vector3f getTranslation() {
        return this.translation;
    }

    public boolean isDriver() {
        return this.driver;
    }
}
