package ca.bkaw.torque;

import ca.bkaw.torque.platform.Platform;
import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class for Torque.
 */
public class Torque {
    private final @NotNull Platform platform;
    private final List<Vehicle> vehicles = new ArrayList<>();

    public Torque(@NotNull Platform platform) {
        this.platform = platform;
    }

    public void setup() {
        this.platform.setup(new TorqueCommand(this));
    }

    public @NotNull Platform getPlatform() {
        return this.platform;
    }

    public void addVehicle(@NotNull Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }
}
