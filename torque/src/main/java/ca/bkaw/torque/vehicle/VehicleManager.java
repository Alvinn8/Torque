package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.components.TestDriveComponent;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.render.VehicleRenderer;
import ca.bkaw.torque.util.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VehicleManager {
    private final @NotNull Torque torque;

    // Registries
    private final Registry<VehicleComponentType> componentTypeRegistry
        = new Registry<>(VehicleComponentType::identifier);

    // Loaded vehicles
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<VehicleRenderer> vehicleRenderers = new ArrayList<>();
    private final Map<Player, Vehicle> currentVehicleMap = new HashMap<>();

    public VehicleManager(Torque torque) {
        this.torque = torque;
        this.registerBuiltIns();
        torque.getPlatform().runEachTick(this::tick);
    }

    private void registerBuiltIns() {
        this.componentTypeRegistry.register(
            RigidBodyComponent.TYPE,
            SeatsComponent.TYPE,
            TestDriveComponent.TYPE
        );
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

    /**
     * Set the vehicle that the passenger is currently in. Or null to remove the passenger from any vehicle.
     *
     * @param passenger The passenger.
     * @param vehicle The vehicle, or null.
     */
    public void setCurrentVehicle(@NotNull Player passenger, @Nullable Vehicle vehicle) {
        if (vehicle != null) {
            this.currentVehicleMap.put(passenger, vehicle);
        } else {
            this.currentVehicleMap.remove(passenger);
        }
    }
}
