package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.platform.ItemDisplay;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.net.InetSocketAddress;
import java.util.UUID;

public record PaperPlayer(org.bukkit.entity.Player entity) implements ca.bkaw.torque.platform.Player {
    @Override
    public void sendResourcePack(UUID id, String url, byte[] hash, boolean required, @Nullable String prompt) {
        this.entity.addResourcePack(id, url, hash, prompt, required);
    }

    @Override
    public @Nullable InetSocketAddress getAddress() {
        return this.entity.getAddress();
    }

    @Override
    public void getInput(@NotNull Input input) {
        var source = this.entity.getCurrentInput();
        input.forward = source.isForward();
        input.backward = source.isBackward();
        input.left = source.isLeft();
        input.right = source.isRight();
        input.jump = source.isJump();
        input.shift = source.isSneak();
        input.sprint = source.isSprint();
    }

    @Override
    public void mountVehicle(ItemDisplay entity) {
        ((PaperItemDisplay) entity).entity().addPassenger(this.entity);
    }

    @Override
    public void dismountVehicle() {
        this.entity.leaveVehicle();
    }

    @Override
    public @NotNull Vector3d getPosition() {
        Location location = this.entity.getLocation();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }
}
