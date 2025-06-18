package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.render.VehicleRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VehicleManager {
    private final @NotNull Torque torque;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<VehicleRenderer> vehicleRenderers = new ArrayList<>();

    public VehicleManager(Torque torque) {
        this.torque = torque;
        torque.getPlatform().runEachTick(this::tick);
    }

    private void tick() {
        for (Vehicle vehicle : this.vehicles) {
            vehicle.tick();
        }
        for (VehicleRenderer vehicleRenderer : this.vehicleRenderers) {
            vehicleRenderer.render();
        }
    }

    public void addVehicle(@NotNull Vehicle vehicle) {
        this.vehicles.add(vehicle);
    }

    public List<Vehicle> getVehicles() {
        return this.vehicles;
    }

    public void startRendering(@NotNull Vehicle vehicle) {
        VehicleRenderer renderer = new VehicleRenderer(vehicle);
        renderer.setup(this.torque);
        this.vehicleRenderers.add(renderer);
    }

    public void stopRendering(@NotNull Vehicle vehicle) {
        this.vehicleRenderers.removeIf(renderer -> renderer.getVehicle().equals(vehicle));
    }
}
