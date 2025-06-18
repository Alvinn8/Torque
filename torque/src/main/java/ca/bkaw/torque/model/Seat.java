package ca.bkaw.torque.model;

import org.joml.Vector3f;

public class Seat {
    public static final double VERTICAL_OFFSET = 0.6; // unit: blocks (meters)

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
        if (false) {
            double periodTime = 5000f; // unit: milliseconds
            float s = 0.8f * (float) Math.sin(System.currentTimeMillis() * 2.0 * Math.PI / periodTime);
            return this.translation.add(new Vector3f(s, 0, 0), new Vector3f());
        }
        if (false) {
            return new Vector3f(0, 0, -10);
        }
        return this.translation;
    }

    public boolean isDriver() {
        return this.driver;
    }
}
