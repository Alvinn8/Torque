package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import ca.bkaw.torque.platform.entity.Player;
import ca.bkaw.torque.platform.World;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.net.InetSocketAddress;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

public record FabricPlayer(ServerPlayer entity) implements Player {
    @Override
    public void sendResourcePack(UUID id, String url, byte[] hash, boolean required, @Nullable String prompt) {
        this.entity.connection.send(new ClientboundResourcePackPushPacket(
            id, url,
            HexFormat.of().formatHex(hash),
            required,
            Optional.ofNullable(prompt).map(Component::literal)
        ));
    }

    @Override
    public InetSocketAddress getAddress() {
        if (this.entity.connection.getRemoteAddress() instanceof InetSocketAddress address) {
            return address;
        }
        return null;
    }


    @Override
    public void getInput(@NotNull Input input) {
        var source = this.entity.getLastClientInput();
        input.forward = source.forward();
        input.backward = source.backward();
        input.left = source.left();
        input.right = source.right();
        input.jump = source.jump();
        input.shift = source.shift();
        input.sprint = source.sprint();
    }

    @Override
    public void mountVehicle(ItemDisplay entity) {
        this.entity.startRiding(((FabricItemDisplay) entity).entity(), true);
    }

    @Override
    public void dismountVehicle() {
        this.entity.stopRiding();
    }

    @Override
    public @NotNull Vector3d getPosition() {
        Vec3 position = this.entity.position();
        return new Vector3d(position.x(), position.y(), position.z());
    }

    @Override
    public @NotNull World getWorld() {
        return new FabricWorld(this.entity.level());
    }

    @Override
    public boolean isInCreativeMode() {
        return this.entity.getAbilities().instabuild;
    }
}
