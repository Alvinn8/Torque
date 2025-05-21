package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.World;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.joml.Vector3d;

public record FabricWorld(ServerLevel level) implements World {
    @Override
    public ItemDisplay spawmItemDisplay(Vector3d position) {
        Display.ItemDisplay entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, this.level);
        entity.setPos(position.x(), position.y(), position.z());
        this.level.addFreshEntity(entity);
        return new FabricItemDisplay(entity);
    }
}
