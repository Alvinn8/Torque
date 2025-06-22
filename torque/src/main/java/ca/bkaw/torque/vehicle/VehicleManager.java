package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.ResourcePack;
import ca.bkaw.torque.components.DragComponent;
import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.components.TestDriveComponent;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.render.VehicleRenderer;
import ca.bkaw.torque.util.Registry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3dc;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class VehicleManager {
    private final @NotNull Torque torque;

    // Registries
    private final Registry<VehicleComponentType> componentTypeRegistry
        = new Registry<>(VehicleComponentType::identifier);
    private final Registry<VehicleType> vehicleTypeRegistry
        = new Registry<>(VehicleType::identifier);

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
            TestDriveComponent.TYPE,
            DragComponent.TYPE
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

    public @NotNull Torque getTorque() {
        return this.torque;
    }

    public Registry<VehicleComponentType> getComponentTypeRegistry() {
        return this.componentTypeRegistry;
    }

    public Registry<VehicleType> getVehicleTypeRegistry() {
        return this.vehicleTypeRegistry;
    }

    public List<Vehicle> getVehicles() {
        return this.vehicles;
    }

    private void startRendering(@NotNull Vehicle vehicle) {
        VehicleRenderer renderer = new VehicleRenderer(vehicle);
        renderer.setup(this.torque);
        this.vehicleRenderers.add(renderer);
    }

    private void stopRendering(@NotNull Vehicle vehicle) {
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

    /**
     * Spawn a new vehicle.
     *
     * @param vehicleType The type of vehicle to spawn.
     * @param world The world to spawn the vehicle in.
     * @param position The position in the world to spawn the vehicle at.
     */
    public void spawnVehicle(@NotNull VehicleType vehicleType, @NotNull World world, @NotNull Vector3dc position) {
        Vehicle vehicle = new Vehicle(this.torque, vehicleType);
        for (VehicleType.ComponentConfiguration configuration : vehicleType.components()) {
            VehicleComponent vehicleComponent = configuration.type().constructor().apply(vehicle, DataInput.empty());
            if (vehicleComponent instanceof RigidBodyComponent rbc) {
                rbc.setWorld(world);
                rbc.setPosition(position);
            }
            vehicle.addComponent(vehicleComponent);
            this.vehicles.add(vehicle);
            this.startRendering(vehicle);
        }
    }

    /**
     * Get the {@link VehicleRenderer} that is currently rendering the given vehicle.
     *
     * @param vehicle The vehicle.
     * @return The renderer for the vehicle, or null if the vehicle is not being rendered.
     */
    @Nullable
    public VehicleRenderer getRenderer(@NotNull Vehicle vehicle) {
        return this.vehicleRenderers.stream()
            .filter(renderer -> renderer.getVehicle().equals(vehicle))
            .findFirst()
            .orElse(null);
    }

    private List<Path> list(Path directory, Predicate<Path> predicate) throws IOException {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(predicate).toList();
        }
    }

    /**
     * Register all vehicle types from the {@code data} directory in the given pack.
     *
     * @param pack The pack that contains the data.
     * @throws IOException If an I/O error occurs.
     */
    public void registerVehicleTypes(@NotNull ResourcePack pack) throws IOException {
        for (Path namespaceFolder : list(pack.getPath("data"), Files::isDirectory)) {
            String namespace = namespaceFolder.getFileName().toString();
            if (!Identifier.validNamespace(namespace)) {
                continue;
            }
            for (Path vehicleTypeFile : list(namespaceFolder.resolve("torque_vehicle"), path -> path.toString().endsWith(".json") && Files.isRegularFile(path))) {
                String key = vehicleTypeFile.getFileName().toString();
                key = key.substring(0, key.length() - ".json".length());
                if (!Identifier.validKey(key)) {
                    continue;
                }
                Identifier identifier = new Identifier(namespace, key);
                JsonObject json;
                try (BufferedReader reader = Files.newBufferedReader(vehicleTypeFile)) {
                    json = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (JsonParseException | IllegalStateException e) {
                    Torque.LOGGER.warning("Invalid vehicle type file: " + vehicleTypeFile + " - " + e.getMessage());
                    continue;
                }
                VehicleType vehicleType = VehicleType.fromJson(this, identifier, json);
                this.vehicleTypeRegistry.register(vehicleType);
            }
        }
    }

    /**
     * Save all vehicles.
     */
    public void saveAll() {
        Iterator<Vehicle> iterator = this.vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            VehicleRenderer vehicleRenderer = this.getRenderer(vehicle);
            if (vehicleRenderer != null) {
                ItemDisplay entity = vehicleRenderer.getPrimaryEntity();
                DataOutput dataOutput = entity.getDataOutput();
                // TODO serialize all components
            }
            iterator.remove();
        }
    }
}
