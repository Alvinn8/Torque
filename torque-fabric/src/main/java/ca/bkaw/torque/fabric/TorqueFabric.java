package ca.bkaw.torque.fabric;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class TorqueFabric implements ModInitializer {
    private static @Nullable TorqueFabric instance;
    private @Nullable FabricPlatform platform;
    private @Nullable Torque torque;
    private @Nullable FabricTorqueCommand command;
    private @Nullable MinecraftServer server;

    @Override
    public void onInitialize() {
        instance = this;
        this.platform = new FabricPlatform();
        this.torque = new Torque(this.platform);

        // Command is registered by a mixin
        this.command = new FabricTorqueCommand(this.platform);
    }

    public static @Nullable TorqueFabric getInstance() {
        return instance;
    }

    public @Nullable FabricPlatform getPlatform() {
        return this.platform;
    }

    public @Nullable MinecraftServer getServer() {
        return this.server;
    }

    public void setServer(@Nullable MinecraftServer server) {
        this.server = server;
    }

    public @Nullable FabricTorqueCommand getCommand() {
        return this.command;
    }

}
