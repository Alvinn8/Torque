package ca.bkaw.torque.assets;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelElementList;
import ca.bkaw.torque.assets.send.BuiltInTcpResourcePackSender;
import ca.bkaw.torque.assets.send.ResourcePackSender;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class TorqueAssets {
    public static final UUID PACK_UUID = UUID.fromString("c73385d8-6493-4124-af7a-62dbd32eb0e5");
    public static final String DESCRIPTION = "Torque";
    public static final int PACK_FORMAT = 55;
    private static final Gson gson = new Gson();

    private final @NotNull Torque torque;
    private @Nullable ResourcePack resourcePack;
    private final @NotNull Path resourcePackPath;
    private byte @Nullable[] sha1;
    private final ResourcePackSender sender;

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

    public static TorqueAssets load(Torque torque) throws IOException {
        // Create resource pack zip
        Path resourcePackPath = Path.of("torque_resource_pack.zip");
        Files.deleteIfExists(resourcePackPath);
        ResourcePack resourcePack = ResourcePack.loadZip(resourcePackPath);
        resourcePack.create(DESCRIPTION, PACK_FORMAT);

        // Read the torque jar file to get the assets from it.
        Path jarPath;
        try {
            jarPath = Path.of(TorqueAssets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to find torque jar file.", e);
        }

        // Merge torque resources in to resource pack
        try (ResourcePack jarResources = ResourcePack.loadZip(jarPath)) {
            resourcePack.include(jarResources, path -> path.startsWith("assets/"));
        }

        return new TorqueAssets(torque, resourcePack, resourcePackPath);
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
        System.out.println("hi");
        System.out.println(this.resourcePack.getPath("assets"));
        System.out.println(Files.list(this.resourcePack.getPath("")).toList());
        System.out.println(Files.list(this.resourcePack.getPath("assets/torque/models")).toList());
        System.out.println(Files.list(this.resourcePack.getPath("assets/torque/models/vehicle")).toList());
        for (Path path : Files.list(this.resourcePack.getPath("assets/torque/models/vehicle")).filter(path -> path.toString().endsWith(".json")).toList()) {
            System.out.println("path = " + path);
            JsonObject json;
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }
            Model model = new Model(json);
            ModelElementList elements = model.getAllElements();
            if (elements == null) {
                continue;
            }
            elements.scale(new Vector3d(1.0 / 10), new Vector3d(8, 0, 8));

            String name = path.getFileName().toString();
            name = name.substring(0, name.length() - ".json".length());
            Path directory = this.resourcePack.getPath("assets/torque/models/vehicle/" + name);
            Files.createDirectories(directory);

            Path primaryPath = directory.resolve("primary.json");
            Files.writeString(primaryPath, gson.toJson(model.getJson()));
            System.out.println("Creating " + primaryPath);

            // Create item model
            JsonObject modelJson = new JsonObject();
            JsonObject modelJson1 = new JsonObject();
            modelJson1.addProperty("type", "minecraft:model");
            modelJson1.addProperty("model", "torque:vehicle/" + name + "/primary");
            modelJson.add("model", modelJson1);
            Path path2 = this.resourcePack.getPath("assets/torque/items/vehicle/" + name + "/primary.json");
            Files.createDirectories(path2.getParent());
            Files.writeString(path2, gson.toJson(modelJson));

            // Delete original to avoid errors in the client logs.
            Files.delete(path);
        }
    }
}
