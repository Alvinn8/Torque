package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.TorqueCommand;
import ca.bkaw.torque.assets.send.BuiltInTcpResourcePackSender;
import ca.bkaw.torque.paper.TorquePaper;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.Platform;
import io.netty.channel.Channel;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class PaperPlatform implements Platform {
    private final TorquePaper plugin;
    private @Nullable TorqueCommand torqueCommand;

    public PaperPlatform(TorquePaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup(@NotNull TorqueCommand torqueCommand) {
        this.torqueCommand = torqueCommand;
    }

    public @Nullable TorqueCommand getTorqueCommand() {
        return this.torqueCommand;
    }

    @Override
    public ItemStack createModelItem(@NotNull Identifier modelIdentifier) {
        Material material = Material.STICK;
        org.bukkit.inventory.ItemStack itemStack = org.bukkit.inventory.ItemStack.of(material);
        NamespacedKey modelKey = new NamespacedKey(modelIdentifier.namespace(), modelIdentifier.key());
        itemStack.editMeta(meta -> meta.setItemModel(modelKey));
        return new PaperItemStack(itemStack);
    }

    @Override
    public void injectChannelHandler(io.netty.channel.ChannelHandler channelHandler, Identifier handlerKey) throws ReflectiveOperationException {
        // Implement the ChannelInitializeListener interface using a proxy
        Class<?> listenerClass = Class.forName("io.papermc.paper.network.ChannelInitializeListener");
        Object listener = Proxy.newProxyInstance(
            BuiltInTcpResourcePackSender.class.getClassLoader(),
            new Class[]{ listenerClass },
            (proxy, method, args) -> {
                if ("afterInitChannel".equals(method.getName())) {
                    Channel channel = (Channel) args[0];
                    channel.pipeline().addFirst(handlerKey.toString(), channelHandler);
                    return null;
                }
                return method.invoke(proxy, args);
            });

        // Add the listener
        Class<?> holderClass = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
        Method method = holderClass.getMethod("addListener", Key.class, listenerClass);
        method.invoke(null, Key.key(handlerKey.namespace(), handlerKey.key()), listener);
    }

    @Override
    public void uninjectChannelHandler(Identifier handlerKey) throws ReflectiveOperationException {
        // Remove the listener
        Class<?> holderClass = Class.forName("io.papermc.paper.network.ChannelInitializeListenerHolder");
        Method method = holderClass.getMethod("removeListener", Key.class);
        method.invoke(null, Key.key(handlerKey.namespace(), handlerKey.key()));
    }

    @Override
    public int getPort() {
        return Bukkit.getPort();
    }

    @Override
    public void runEachTick(@NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, 1L, 1L);
    }
}
