package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.fabric.ItemDisplayAccessor;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.World;
import com.mojang.math.Transformation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

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
    public World getWorld() {
        return new FabricWorld((ServerLevel) this.entity.level());
    }

    @Override
    public @NotNull Vector3d getPosition() {
        Vec3 position = this.entity.position();
        return new Vector3d(position.x(), position.y(), position.z());
    }

    @Override
    public void setPosition(Vector3dc position) {
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
    public boolean isAlive() {
        return this.entity.isAlive();
    }

    @Override
    public DataInput getDataInput() {
        CompoundTag torqueData = ((ItemDisplayAccessor) this.entity).torque$getTorqueData();
        if (torqueData == null) {
            return DataInput.empty();
        }
        return new NbtInputOutput(torqueData);
    }

    @Override
    public DataOutput getDataOutput() {
        CompoundTag torqueData = ((ItemDisplayAccessor) this.entity).torque$getTorqueData();
        if (torqueData == null) {
            torqueData = new CompoundTag();
            ((ItemDisplayAccessor) this.entity).torque$setTorqueData(torqueData);
        }
        return new NbtInputOutput(torqueData);
    }

    @Override
    public void mountVehicle(@NotNull ItemDisplay entity) {
        this.entity.startRiding(((FabricItemDisplay) entity).entity(), true);
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.entity.setGlowingTag(glowing);
    }

    @Override
    public void setGlowColor(int glowColor) {
        this.entity.setGlowColorOverride(glowColor);
    }
}
