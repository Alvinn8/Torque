package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.fabric.TorqueFabric;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.Platform;
import io.netty.channel.ChannelHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class FabricPlatform implements Platform {
    private @Nullable TorqueCommand torqueCommand;
    private final @NotNull Map<Identifier, ChannelHandler> channelHandlers = new HashMap<>(1);

    @Override
    public void setup(@NotNull TorqueCommand torqueCommand) {
        this.torqueCommand = torqueCommand;
    }

    public @Nullable TorqueCommand getTorqueCommand() {
        return this.torqueCommand;
    }

    public @NotNull Map<Identifier, ChannelHandler> getChannelHandlers() {
        return this.channelHandlers;
    }

    @Override
    public ItemStack createModelItem(@NotNull Identifier modelIdentifier) {
        net.minecraft.world.item.ItemStack itemStack = new net.minecraft.world.item.ItemStack(Items.STICK);
        ResourceLocation modelKey = ResourceLocation.fromNamespaceAndPath(modelIdentifier.namespace(), modelIdentifier.key());
        itemStack.applyComponents(DataComponentMap.builder().set(DataComponents.ITEM_MODEL, modelKey).build());
        return new FabricItemStack(itemStack);
    }

    @Override
    public void injectChannelHandler(ChannelHandler channelHandler, Identifier handlerKey) {
        this.channelHandlers.put(handlerKey, channelHandler);
    }

    @Override
    public void uninjectChannelHandler(Identifier handlerKey) {
        this.channelHandlers.remove(handlerKey);
    }

    @Override
    public int getPort() {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            throw new IllegalStateException("Server not started yet.");
        }
        MinecraftServer server = torqueFabric.getServer();
        if (server == null) {
            throw new IllegalStateException("Server not started yet.");
        }
        return server.getPort();
    }

    @Override
    public void runEachTick(@NotNull Runnable runnable) {
        ServerTickEvents.START_SERVER_TICK.register(server -> runnable.run());
    }
}
