package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.ResourcePack;
import ca.bkaw.torque.components.CollisionComponent;
import ca.bkaw.torque.components.DragComponent;
import ca.bkaw.torque.components.FloatComponent;
import ca.bkaw.torque.components.GravityComponent;
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
    private final Map<ItemDisplay, Vehicle> vehiclePartMap = new HashMap<>();

    public static boolean tickStep = false;

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
            DragComponent.TYPE,
            GravityComponent.TYPE,
            FloatComponent.TYPE,
            CollisionComponent.TYPE
        );
    }

    private void tick() {
        // if (!tickStep) {
        //     return;
        // }
        // tickStep = false;
        for (Vehicle vehicle : this.vehicles) {
            vehicle.tick();
        }
        Iterator<VehicleRenderer> iter = this.vehicleRenderers.iterator();
        while (iter.hasNext()) {
            VehicleRenderer vehicleRenderer = iter.next();
            vehicleRenderer.render();
            if (!vehicleRenderer.getPrimaryEntity().isAlive()) {
                // If we remove first, we can avoid a ConcurrentModificationException
                iter.remove();
                this.stopRendering(vehicleRenderer.getVehicle());
                this.vehicles.remove(vehicleRenderer.getVehicle());
                System.out.println("Removing a vehicle");
            }
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

    private void startRendering(@NotNull Vehicle vehicle, @NotNull ItemDisplay primaryEntity) {
        VehicleRenderer renderer = new VehicleRenderer(vehicle, primaryEntity);
        renderer.setup(this.torque);
        this.vehicleRenderers.add(renderer);
    }

    private void stopRendering(@NotNull Vehicle vehicle) {
        this.vehicleRenderers.removeIf(renderer -> renderer.getVehicle().equals(vehicle));
    }

    /**
     * Set the vehicle that the passenger is currently in. Or null to remove the
     * passenger from any vehicle.
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
        }
        this.vehicles.add(vehicle);
        System.out.println("position = " + position);
        ItemDisplay primaryEntity = world.spawnItemDisplay(position);
        this.vehiclePartMap.put(primaryEntity, vehicle);
        this.startRendering(vehicle, primaryEntity);
    }

    /**
     * Save the state of the vehicle to persistent storage on the primary entity.
     *
     * @param vehicle The vehicle to save.
     */
    public void saveVehicle(@NotNull Vehicle vehicle) {
        VehicleRenderer vehicleRenderer = this.getRenderer(vehicle);
        if (vehicleRenderer == null) {
            return;
        }
        ItemDisplay entity = vehicleRenderer.getPrimaryEntity();
        DataOutput dataOutput = entity.getDataOutput();
        dataOutput.writeIdentifier("vehicle_type", vehicle.getType().identifier());
        DataOutput componentsData = dataOutput.getOrCreateDataOutput("components");
        for (VehicleComponent component : vehicle.getComponents()) {
            DataOutput componentData = componentsData.getOrCreateDataOutput(
                component.getType().identifier().toString()
            );
            component.save(vehicle, componentData);
            componentData.save();
        }
        componentsData.save();
        dataOutput.save();
    }

    /**
     * Save all vehicles.
     */
    public void saveAll() {
        this.vehicles.forEach(this::saveVehicle);
    }

    /**
     * Save all vehicles and unload them from the world.
     */
    public void saveAndUnloadAll() {
        this.saveAll();
        Iterator<Vehicle> iterator = this.vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            this.stopRendering(vehicle);
            iterator.remove();
        }
    }

    /**
     * Load a vehicle from the data stored in the persistent storage in the primary
     * entity.
     *
     * @param primaryEntity The primary entity that contains the vehicle data.
     */
    public void loadVehicle(@NotNull ItemDisplay primaryEntity) {
        DataInput data = primaryEntity.getDataInput();
        Identifier vehicleTypeIdentifier = data.readIdentifier("vehicle_type", null);
        VehicleType vehicleType = this.vehicleTypeRegistry.get(vehicleTypeIdentifier);
        if (vehicleType == null) {
            return;
        }
        Vehicle vehicle = new Vehicle(this.torque, vehicleType);
        DataInput componentsData = data.getDataInput("components");
        for (VehicleType.ComponentConfiguration component : vehicleType.components()) {
            DataInput componentData = componentsData.getDataInput(component.type().identifier().toString());
            VehicleComponent vehicleComponent = component.type().constructor().apply(vehicle, componentData);
            vehicle.addComponent(vehicleComponent);
            if (vehicleComponent instanceof RigidBodyComponent rbc) {
                rbc.setWorld(primaryEntity.getWorld());
            }
        }
        this.vehicles.add(vehicle);
        this.startRendering(vehicle, primaryEntity);
    }

    /**
     * Get the vehicle that an item display entity is rendering.
     *
     * @param entity The item display entity.
     * @return The vehicle, or null if the entity is not part of a vehicle.
     */
    @Nullable
    public Vehicle getVehicleFromPart(@NotNull ItemDisplay entity) {
        return this.vehiclePartMap.get(entity);
    }

    /**
     * Set the vehicle that an item display entity is part of.
     *
     * @param entity The item display entity.
     * @param vehicle The vehicle.
     */
    public void setVehiclePart(@NotNull ItemDisplay entity, @Nullable Vehicle vehicle) {
        if (vehicle == null) {
            this.vehiclePartMap.remove(entity);
            return;
        }
        this.vehiclePartMap.put(entity, vehicle);
    }
}
