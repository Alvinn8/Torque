package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.ItemStack;
import com.mojang.math.Transformation;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public record FabricItemDisplay(Display.ItemDisplay entity) implements ItemDisplay {
    @Override
    public void setItem(ItemStack item) {
        this.entity.getSlot(0).set(((FabricItemStack) item).itemStack());
    }

    @Override
    public void setTransformation(Matrix4f affineTransformMatrix) {
        this.entity.setTransformation(new Transformation(affineTransformMatrix));
    }

    @Override
    public void setPosition(Vector3d position) {
        this.entity.setPos(position.x(), position.y(), position.z());
    }

    @Override
    public void setTeleportDuration(int ticks) {
        this.entity.setPosRotInterpolationDuration(ticks);
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        this.entity.setTransformationInterpolationDuration(ticks);
    }

    @Override
    public void setStartInterpolation(int ticks) {
        this.entity.setTransformationInterpolationDelay(ticks);
    }

    @Override
    public void remove() {
        this.entity.remove(Entity.RemovalReason.KILLED);
    }

    @Override
    public DataInput getDataInput() {
        return null;
    }

    @Override
    public DataOutput getDataOutput() {
        return null;
    }
}
