package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.ItemStack;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public record PaperItemDisplay(org.bukkit.entity.ItemDisplay entity) implements ca.bkaw.torque.platform.ItemDisplay {
    @Override
    public void setItem(ItemStack item) {
        this.entity.setItemStack(((PaperItemStack) item).itemStack());
    }

    @Override
    public void setTransformation(Matrix4f affineTransformMatrix) {
        this.entity.setTransformationMatrix(affineTransformMatrix);
    }

    @Override
    public void setPosition(Vector3d position) {
        this.entity.teleport(
            new Location(this.entity.getWorld(), position.x(), position.y(), position.z()),
            TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
    }

    @Override
    public void setTeleportDuration(int ticks) {
        this.entity.setTeleportDuration(ticks);
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        this.entity.setInterpolationDuration(ticks);
    }

    @Override
    public void setStartInterpolation(int ticks) {
        this.entity.setInterpolationDelay(ticks);
    }

    @Override
    public void remove() {
        this.entity.remove();
    }
}
