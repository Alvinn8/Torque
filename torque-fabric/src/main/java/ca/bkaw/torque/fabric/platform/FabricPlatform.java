package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.PlatformEvents;
import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.fabric.TorqueFabric;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.Platform;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import io.netty.channel.ChannelHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FabricPlatform implements Platform {
    private @Nullable TorqueCommand torqueCommand;
    private @Nullable PlatformEvents eventHandler;
    private final @NotNull Map<Identifier, ChannelHandler> channelHandlers = new HashMap<>(1);

    @Override
    public void setup(@NotNull TorqueCommand torqueCommand, @NotNull PlatformEvents eventHandler) {
        this.torqueCommand = torqueCommand;
        this.eventHandler = eventHandler;
    }

    public @Nullable TorqueCommand getTorqueCommand() {
        return this.torqueCommand;
    }

    public @Nullable PlatformEvents getEventHandler() {
        return this.eventHandler;
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

    @Override
    public List<ItemDisplay> getAllItemDisplays() {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return List.of();
        }
        MinecraftServer server = torqueFabric.getServer();
        if (server == null) {
            return List.of();
        }
        List<ItemDisplay> list = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Display.ItemDisplay entity : level.getEntities(EntityType.ITEM_DISPLAY, Entity::isAlive)) {
                list.add(new FabricItemDisplay(entity));
            }
        }
        return list;
    }
}
