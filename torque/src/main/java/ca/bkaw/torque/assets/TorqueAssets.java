package ca.bkaw.torque.assets;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelElementList;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.send.BuiltInTcpResourcePackSender;
import ca.bkaw.torque.assets.send.ResourcePackSender;
import ca.bkaw.torque.model.TagHandler;
import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.model.VehicleModelPart;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.util.InertiaTensor;
import ca.bkaw.torque.util.Registry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TorqueAssets {
    public static final UUID PACK_UUID = UUID.fromString("c73385d8-6493-4124-af7a-62dbd32eb0e5");
    public static final String DESCRIPTION = "Torque";
    public static final int PACK_FORMAT = 55;
    private static final Gson GSON = new Gson();

    private @NotNull final Torque torque;
    private @Nullable ResourcePack resourcePack;
    private final @NotNull Path resourcePackPath;
    private byte @Nullable[] sha1;
    private final ResourcePackSender sender;

    private final Registry<VehicleModel> vehicleModelRegistry = new Registry<>(VehicleModel::getIdentifier);

    private TorqueAssets(@NotNull Torque torque, @NotNull ResourcePack resourcePack, @NotNull Path resourcePackPath) {
        this.torque = torque;
        this.resourcePack = resourcePack;
        this.resourcePackPath = resourcePackPath;
        try {
            this.sender = new BuiltInTcpResourcePackSender(torque);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set up built-in TCP resource pack sender.", e);
        }
    }

    public static TorqueAssets createPack(Torque torque) throws IOException {
        // Create resource pack zip
        Path resourcePackPath = Path.of("torque_resource_pack.zip");
        Files.deleteIfExists(resourcePackPath);
        ResourcePack resourcePack = ResourcePack.loadZip(resourcePackPath);
        resourcePack.create(DESCRIPTION, PACK_FORMAT);

        return new TorqueAssets(torque, resourcePack, resourcePackPath);
    }

    public static ResourcePack getJarResources(Class<?> clazz) throws IOException {
        // Read the jar file to get the resources from it.
        Path jarPath;
        try {
            jarPath = Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find torque jar file.", e);
        }
        return ResourcePack.loadZip(jarPath);
    }

    /**
     * Include the assets folder from the specified resource pack.
     *
     * @param assets The resource pack to include assets from.
     * @throws IOException If an I/O error occurs.
     */
    public void includeAssets(ResourcePack assets) throws IOException {
        if (this.resourcePack == null) {
            throw new IllegalStateException("Already saved");
        }
        this.resourcePack.include(assets, path -> path.startsWith("assets"));
    }

    public void save() throws IOException {
        if (this.resourcePack == null) {
            throw new IllegalStateException("Already saved");
        }
        // Close resource pack
        this.resourcePack.close();
        this.resourcePack = null;

        // Get the file SHA-1 hash of resource pack
        try {
            this.sha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(this.resourcePackPath));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Should never happen. JVM must implement SHA-1.
        }
    }

    public @NotNull Path getResourcePackPath() {
        return this.resourcePackPath;
    }

    public byte @Nullable[] getSha1Hash() {
        return this.sha1;
    }

    public ResourcePackSender getSender() {
        return this.sender;
    }

    /**
     * Convert the position of a point in the vehicle model's coordinate system to
     * an offset from the model's center measured in meters.
     * <p>
     * This can be used to get the offset of an element relative to the vehicle's
     * center of mass.
     *
     * @param modelPosition The vector in the vehicle model's coordinate system.
     *                      Unit: Model units ("pixels").
     * @return The vector in world coordinates. Unit: meters.
     */
    public static @NotNull Vector3d getElementOffset(@NotNull Vector3dc modelPosition) {
        // The element position is in model units ("pixels"), so we need to convert it
        // to blocks. 1 block = 16 pixels.
        // The model is centered around (8, 8, 8) in the model, so we need to find
        // the difference from that center.
        return new Vector3d(modelPosition).sub(8, 8, 8).div(16);
    }

    /**
     * Get a vehicle model if it has already been created, or create it from the JSON
     * file if it does not exist.
     *
     * @param identifier The identifier of the JSON file. The JSON file should be located at
     *                   "assets/{namespace}/models/{key}.json".
     * @return The vehicle model, or null if it does not exist or cannot be created.
     * @throws IOException If an I/O error occurs.
     */
    @Nullable
    public VehicleModel getOrCreateVehicleModel(Identifier identifier) throws IOException {
        if (this.resourcePack == null) {
            throw new IllegalStateException("Cannot create vehicle model now.");
        }

        VehicleModel existing = this.vehicleModelRegistry.get(identifier);
        if (existing != null) {
            return existing;
        }

        Path path = this.resourcePack.getPath(
            "assets/" + identifier.namespace() + "/models/" + identifier.key() + ".json"
        );
        if (Files.notExists(path)) {
            return null;
        }
        JsonObject json;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Model model = new Model(json);
        ModelElementList elements = model.getAllElements();
        if (elements == null) {
            // No elements found, cannot create a vehicle model.
            return null;
        }

        // Center on center of mass.
        Vector3d centerOfMass = InertiaTensor.getCenterOfMass(elements);
        // The center of mass is calculated in world space, so convert back to pixels by
        // multiplying by 16 because 16 pixels = 1 meter.
        // The center is at (8, 8, 8) in the model.
        Vector3d diff = new Vector3d(8, 8, 8).sub(new Vector3d(centerOfMass).mul(16));
        elements.move(diff);

        Model modelToKeep = model.deepCopy();

        ModelExtractor modelExtractor = new ModelExtractor(model);

        // Process tags
        Map<Class<? extends TagHandler<?>>, Object> tagHandlerData = new HashMap<>();
        for (TagHandler<?> tagHandler : this.torque.getVehicleManager().getTagHandlers()) {
            Object result = tagHandler.process(model, modelExtractor);
            if (result != null) {
                @SuppressWarnings("unchecked")
                Class<? extends TagHandler<?>> handlerClass = (Class<? extends TagHandler<?>>) tagHandler.getClass();
                tagHandlerData.put(handlerClass, result);
            }
        }

        // Perform extraction to get model parts.
        Map<String, Model> modelParts = modelExtractor.executeExtractions();

        // Save primary model

        // Treat elements with a leading dot as hidden elements.
        elements.removeIf(el -> String.valueOf(el.getName()).startsWith("."));

        // The game only allows a block size of 3.
        // Scale down so it fits.
        double originalBlockSize = elements.getBlockSize();
        double scale = 1.0;
        if (originalBlockSize > 3.0) {
            scale = 3.0 / originalBlockSize;
            elements.scale(new Vector3d(scale), new Vector3d(8, 8, 8));
        }

        Path directory = this.resourcePack.getPath(
            "assets/" + identifier.namespace() + "/models/" + identifier.key()
        );
        Files.createDirectories(directory);

        Path primaryPath = directory.resolve("primary.json");
        Files.writeString(primaryPath, GSON.toJson(model.getJson()));

        // Create an item model
        Identifier primaryModelIdentifier = new Identifier(identifier.namespace(), identifier.key() + "/primary");
        this.createItemModel(primaryModelIdentifier);

        VehicleModelPart primary = new VehicleModelPart(
            "primary",
            primaryModelIdentifier,
            // The vehicle model should scale up to the original size.
            (float) (1.0 / scale),
            // The vehicle should be moved back vertically so that it is
            // grounded at right level.
            // Convert model units ("pixels") to blocks by dividing by 16.
            // Also add 0.5 blocks because the model is centered around (8, 8, 8) in
            // the model, so we need to move it down by half a block (8 model units, "pixels")
            // so that the bottom of the model is at ground level.
            new Vector3f(0, (float) -diff.y / 16.0f + 0.5f, 0)
        );

        // Save model parts
        List<VehicleModelPart> vehicleModelParts = new ArrayList<>();
        for (Map.Entry<String, Model> entry : modelParts.entrySet()) {
            String partName = entry.getKey();
            Model partModel = entry.getValue();
            ModelElementList partElements = partModel.getAllElements();
            if (partElements == null) {
                continue;
            }

            // Remove hidden elements
            partElements.removeIf(el -> String.valueOf(el.getName()).startsWith("."));

            // Center
            Vector3d partMovedBy = partElements.centerGeometrically();

            // Scale to size
            double partOriginalBlockSize = partElements.getBlockSize();
            double partScale = 1.0;
            if (partOriginalBlockSize > 3.0) {
                partScale = 3.0 / partOriginalBlockSize;
                partElements.scale(new Vector3d(partScale), new Vector3d(8, 8, 8));
            }
            Path partPath = directory.resolve(partName + ".json");
            Files.writeString(partPath, GSON.toJson(partModel.getJson()));
            Identifier modelIdentifier = new Identifier(identifier.namespace(), identifier.key() + "/" + partName);
            this.createItemModel(modelIdentifier);
            vehicleModelParts.add(new VehicleModelPart(
                partName,
                modelIdentifier,
                (float) (1.0 / partScale),
                // Convert model units ("pixels") to blocks by dividing by 16.
                new Vector3f(partMovedBy).div(16).negate().add(primary.translation())
            ));
        }

        // Delete original to avoid errors in the client logs.
        Files.delete(path);

        VehicleModel vehicleModel = new VehicleModel(
            identifier,
            modelToKeep,
            primary,
            vehicleModelParts,
            tagHandlerData
        );

        this.vehicleModelRegistry.register(vehicleModel);
        return vehicleModel;
    }

    /**
     * Creates an item model JSON file that links to the specified model.
     *
     * @param identifier The identifier of the model.
     * @throws IOException If an I/O error occurs
     */
    private void createItemModel(Identifier identifier) throws IOException {
        if (this.resourcePack == null) {
            throw new IllegalStateException("Cannot create item model now.");
        }
        
        JsonObject json = new JsonObject();
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("type", "minecraft:model");
        modelJson.addProperty("model", identifier.namespace() + ":" + identifier.key());
        json.add("model", modelJson);
        
        Path itemPath = this.resourcePack.getPath("assets/" + identifier.namespace() + "/items/" + identifier.key() + ".json");
        Files.createDirectories(itemPath.getParent());
        Files.writeString(itemPath, GSON.toJson(json));
    }
}