package ca.bkaw.torque.fabric;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class TorqueFabric implements ModInitializer {
    private static @Nullable TorqueFabric instance;
    private @Nullable FabricPlatform platform;
    private @Nullable Torque torque;
    private @Nullable MinecraftServer server;

    @Override
    public void onInitialize() {
        instance = this;
        this.platform = new FabricPlatform();
        this.torque = new Torque(this.platform);

        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);

        FabricTorqueCommand command = new FabricTorqueCommand(this.platform);
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, ctx ,sel) -> command.register(dispatcher)
        );
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

    public @Nullable Torque getTorque() {
        return this.torque;
    }
}
