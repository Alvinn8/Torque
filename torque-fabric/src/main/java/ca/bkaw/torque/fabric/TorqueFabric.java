package ca.bkaw.torque.fabric;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import org.jetbrains.annotations.Nullable;

public class TorqueFabric implements ModInitializer {
    private static @Nullable TorqueFabric instance;
    private @Nullable FabricTorqueCommand command;

    @Override
    public void onInitialize() {
        instance = this;
        FabricPlatform platform = new FabricPlatform();
        Torque torque = new Torque(platform);
        torque.setup();

        // Command is registered by a mixin
        this.command = new FabricTorqueCommand(platform);
    }

    public static @Nullable TorqueFabric getInstance() {
        return instance;
    }

    public @Nullable FabricTorqueCommand getCommand() {
        return this.command;
    }
}
