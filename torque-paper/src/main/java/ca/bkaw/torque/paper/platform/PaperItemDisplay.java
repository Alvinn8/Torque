package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.ItemStack;
import ca.bkaw.torque.platform.World;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record PaperItemDisplay(org.bukkit.entity.ItemDisplay entity) implements ca.bkaw.torque.platform.ItemDisplay {
    public static final NamespacedKey PDC_VEHICLE_KEY = new NamespacedKey("torque", "vehicle");

    @Override
    public void setItem(ItemStack item) {
        this.entity.setItemStack(((PaperItemStack) item).itemStack());
    }

    @Override
    public void setTransformation(Matrix4f affineTransformMatrix) {
        this.entity.setTransformationMatrix(affineTransformMatrix);
    }

    @Override
    public World getWorld() {
        return new PaperWorld(this.entity.getWorld());
    }

    @Override
    public @NotNull Vector3d getPosition() {
        Location location = this.entity.getLocation();
        return new Vector3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public void setPosition(Vector3dc position) {
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

    @Override
    public boolean isAlive() {
        return this.entity.isValid();
    }

    @Override
    public DataInput getDataInput() {
        PersistentDataContainer pdc = this.entity.getPersistentDataContainer();
        PersistentDataContainer vehicleData = pdc.get(PDC_VEHICLE_KEY, PersistentDataType.TAG_CONTAINER);
        if (vehicleData == null) {
            return DataInput.empty();
        }
        return new PdcInputOutput(vehicleData, pdc, PDC_VEHICLE_KEY);
    }

    @Override
    public DataOutput getDataOutput() {
        PersistentDataContainer pdc = this.entity.getPersistentDataContainer();
        PersistentDataContainer vehicleData = pdc.get(PDC_VEHICLE_KEY, PersistentDataType.TAG_CONTAINER);
        if (vehicleData == null) {
            vehicleData = pdc.getAdapterContext().newPersistentDataContainer();
            pdc.set(PDC_VEHICLE_KEY, PersistentDataType.TAG_CONTAINER, vehicleData);
        }
        return new PdcInputOutput(vehicleData, pdc, PDC_VEHICLE_KEY);
    }

    @Override
    public void mountVehicle(@NotNull ItemDisplay entity) {
        ((PaperItemDisplay) entity).entity().addPassenger(this.entity);
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.entity.setGlowing(glowing);
    }

    @Override
    public void setGlowColor(int glowColor) {
        this.entity.setGlowColorOverride(Color.fromRGB(glowColor));
    }
}
