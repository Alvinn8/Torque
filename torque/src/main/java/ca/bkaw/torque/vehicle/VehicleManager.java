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

    private void tick() {
        for (Vehicle vehicle : this.vehicles) {
            vehicle.tick();
        }
    }

    public void addVehicle(@NotNull Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return this.vehicles;
    }
}
