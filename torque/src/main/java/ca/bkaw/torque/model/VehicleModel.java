package ca.bkaw.torque.model;

import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.List;

public class VehicleModel {
    private VehicleModelPart inline;
    private VehicleModelPart primary;
    private List<VehicleModelPart> parts;
    private double scale;
    private @NotNull Vector3f translation;
    private @NotNull List<Seat> seats;

    public VehicleModel(double scale, @NotNull Vector3f translation, @NotNull List<Seat> seats) {
        this.scale = scale;
        this.translation = translation;
        this.seats = seats;
    }

    public double getScale() {
        return this.scale;
    }

    public Vector3f getTranslation() {
        return this.translation;
    }

    public @NotNull List<Seat> getSeats() {
        return this.seats;
    }
}
