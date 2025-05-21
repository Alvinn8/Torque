package ca.bkaw.torque.render;

import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;

public class VehicleRenderer {
    private @NotNull Vehicle vehicle;

    public VehicleRenderer(@NotNull Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
