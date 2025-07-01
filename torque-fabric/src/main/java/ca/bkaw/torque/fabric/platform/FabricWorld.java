package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

public record FabricWorld(ServerLevel level) implements World {
    @Override
    @NotNull
    public ItemDisplay spawnItemDisplay(@NotNull Vector3dc position) {
        Display.ItemDisplay entity = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, this.level);
        entity.setPos(position.x(), position.y(), position.z());
        this.level.addFreshEntity(entity);
        return new FabricItemDisplay(entity);
    }

    @Override
    public @NotNull BlockState getBlock(@NotNull Vector3ic position) {
        return new FabricBlockState(this.level.getBlockState(new BlockPos(position.x(), position.y(), position.z())));
    }
}
