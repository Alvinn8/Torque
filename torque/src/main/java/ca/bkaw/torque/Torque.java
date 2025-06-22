package ca.bkaw.torque;

import ca.bkaw.torque.assets.ResourcePack;
import ca.bkaw.torque.assets.TorqueAssets;
import ca.bkaw.torque.platform.Platform;
import ca.bkaw.torque.vehicle.VehicleManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Main class for Torque.
 */
public class Torque {
    public static Logger LOGGER = Logger.getLogger("Torque");
    private final @NotNull Platform platform;
    private @NotNull TorqueAssets assets;
    private final @NotNull VehicleManager vehicleManager;

    public Torque(@NotNull Platform platform) {
        this.platform = platform;
        this.platform.setup(new TorqueCommand(this));
        this.vehicleManager = new VehicleManager(this);
        this.reload();
    }

    public @NotNull Platform getPlatform() {
        return this.platform;
    }

    public @NotNull TorqueAssets getAssets() {
        return this.assets;
    }

    public @NotNull VehicleManager getVehicleManager() {
        return this.vehicleManager;
    }

    public void reload() {
        String assetsOverride = System.getProperty("torque.assets");
        try (ResourcePack jarResources = assetsOverride != null
            ? ResourcePack.loadDirectory(Path.of(assetsOverride))
            : TorqueAssets.getJarResources(Torque.class)
        ) {
            this.assets = TorqueAssets.createPack(this);
            this.assets.includeAssets(jarResources);
            this.assets.createVehicleModels();
            this.assets.save();
            this.vehicleManager.saveAll();
            this.vehicleManager.getVehicleTypeRegistry().clear();
            this.vehicleManager.registerVehicleTypes(jarResources);
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up Torque assets.", e);
        }
    }
}
