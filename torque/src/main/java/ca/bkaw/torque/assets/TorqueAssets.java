package ca.bkaw.torque.assets;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelElement;
import ca.bkaw.torque.assets.model.ModelElementList;
import ca.bkaw.torque.assets.send.BuiltInTcpResourcePackSender;
import ca.bkaw.torque.assets.send.ResourcePackSender;
import ca.bkaw.torque.model.Seat;
import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.util.Registry;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class TorqueAssets {
    public static final UUID PACK_UUID = UUID.fromString("c73385d8-6493-4124-af7a-62dbd32eb0e5");
    public static final String DESCRIPTION = "Torque";
    public static final int PACK_FORMAT = 55;
    private static final Gson gson = new Gson();

    private @Nullable ResourcePack resourcePack;
    private final @NotNull Path resourcePackPath;
    private byte @Nullable[] sha1;
    private final ResourcePackSender sender;

    private final Registry<VehicleModel> vehicleModelRegistry = new Registry<>(VehicleModel::getIdentifier);

    private TorqueAssets(@NotNull Torque torque, @NotNull ResourcePack resourcePack, @NotNull Path resourcePackPath) {
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

        // Get file sha1 hash of resource pack
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

    public void createVehicleModels() throws IOException {
        if (this.resourcePack == null) {
            throw new IllegalStateException("Cannot create vehicle models now.");
        }

        this.vehicleModelRegistry.clear();

        List<Path> files;
        try (Stream<Path> stream = Files.list(this.resourcePack.getPath("assets/torque/models/vehicle"))) {
            files = stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json")).toList();
        }
        for (Path path : files) {
            String key = path.getFileName().toString();
            key = key.substring(0, key.length() - ".json".length());
            if (!Identifier.validKey(key)) {
                continue;
            }

            JsonObject json;
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            Model model = new Model(json);
            ModelElementList elements = model.getAllElements();
            if (elements == null) {
                continue;
            }

            // Center geometrically to maximize the space used.
            Vector3d centerDiff = elements.centerGeometrically();

            // Find tagged elements
            List<Seat> seats = new ArrayList<>();
            for (ModelElement element : elements.getElements()) {
                Set<String> tags = element.getTags();
                if (tags.contains("seat")) {
                    boolean isDriver = tags.contains("driver");
                    // Use the middle as the seat position, however vertically we want to use the lowest point.
                    Vector3d seatPosition = element.getMiddle();
                    seatPosition.y = Math.min(element.getFrom().y, element.getTo().y);
                    seatPosition.div(16); // Convert from model units ("pixels") to blocks.
                    seatPosition.add(0, Seat.VERTICAL_OFFSET, 0);

                    seats.add(new Seat(
                        new Vector3f(seatPosition),
                        isDriver
                    ));
                }
            }

            // Treat elements with a leading dot as hidden elements.
            elements.removeIf(el -> String.valueOf(el.getName()).startsWith("."));

            // The game only allows a block size of 3.
            // Scale down so it fits.
            double originalBlockSize = elements.getBlockSize();
            double scale = 1.0;
            if (originalBlockSize > 3.0) {
                scale = 3.0 / originalBlockSize;
                elements.scale(new Vector3d(scale), new Vector3d(8, 0, 8));
            }

            String name = path.getFileName().toString();
            name = name.substring(0, name.length() - ".json".length());
            Path directory = this.resourcePack.getPath("assets/torque/models/vehicle/" + name);
            Files.createDirectories(directory);

            Path primaryPath = directory.resolve("primary.json");
            Files.writeString(primaryPath, gson.toJson(model.getJson()));

            // Create an item model
            this.createItemModel(new Identifier("torque", "vehicle/" + name + "/primary"));

            // Delete original to avoid errors in the client logs.
            Files.delete(path);

            VehicleModel vehicleModel = new VehicleModel(
                new Identifier("torque", "vehicle/" + name),
                // The vehicle model should scale up to the original size.
                1.0 / scale,
                // The vehicle should be moved back vertically so that it is grounded at right level.
                // Convert model units ("pixels") to blocks by dividing by 16.
                new Vector3f(0, (float) -centerDiff.y / 16.0f, 0),
                seats
            );
            this.vehicleModelRegistry.register(vehicleModel);
        }
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
        Files.writeString(itemPath, gson.toJson(json));
    }

    public Registry<VehicleModel> getVehicleModelRegistry() {
        return this.vehicleModelRegistry;
    }
}