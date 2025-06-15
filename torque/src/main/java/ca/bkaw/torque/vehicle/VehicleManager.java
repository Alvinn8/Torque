package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VehicleManager {
    private final List<Vehicle> vehicles = new ArrayList<>();

    public VehicleManager(Torque torque) {
        torque.getPlatform().runEachTick(this::tick);
    }

    public void addVehicle(@NotNull Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }

    private void tick() {
        for (Vehicle vehicle : this.vehicles) {
            vehicle.tick();
        }
    }
}
