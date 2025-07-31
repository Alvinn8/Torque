package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.platform.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.List;

public class VehicleModel {
    private final Identifier identifier;
    private @Nullable Model model;
    private VehicleModelPart inline;
    private VehicleModelPart hologram;
    private VehicleModelPart primary;
    private List<VehicleModelPart> parts;
    private double scale;
    private @NotNull Vector3fc translation;
    private @NotNull List<Seat> seats;

    public VehicleModel(Identifier identifier, @NotNull Model model, List<VehicleModelPart> parts, double scale, @NotNull Vector3fc translation, @NotNull List<Seat> seats) {
        this.identifier = identifier;
        this.model = model;
        this.parts = parts;
        this.scale = scale;
        this.translation = translation;
        this.seats = seats;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }

    public double getScale() {
        return this.scale;
    }

    public @NotNull Vector3fc getTranslation() {
        return this.translation;
    }

    public List<VehicleModelPart> getParts() {
        return this.parts;
    }

    public @NotNull List<Seat> getSeats() {
        return this.seats;
    }

    public @Nullable Model getModel() {
        return this.model;
    }

    public void unsetModel() {
        this.model = null;
    }
}
