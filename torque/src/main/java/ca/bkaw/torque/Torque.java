package ca.bkaw.torque;

import ca.bkaw.torque.assets.TorqueAssets;
import ca.bkaw.torque.platform.Platform;
import ca.bkaw.torque.vehicle.VehicleManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Main class for Torque.
 */
public class Torque {
    public static Logger LOGGER = Logger.getLogger("Torque");
    private final @NotNull Platform platform;
    private final @NotNull TorqueAssets assets;
    private final @NotNull VehicleManager vehicleManager;

    public Torque(@NotNull Platform platform) {
        this.platform = platform;
        try {
            this.assets = TorqueAssets.load(this);
            this.assets.createVehicleModels();
            this.assets.save();
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up Torque assets.", e);
        }
        this.platform.setup(new TorqueCommand(this));
        this.vehicleManager = new VehicleManager(this);
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
}
