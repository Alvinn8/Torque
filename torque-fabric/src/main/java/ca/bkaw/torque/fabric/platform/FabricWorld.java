package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.entity.InteractionEntity;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import ca.bkaw.torque.platform.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.shapes.VoxelShape;
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
    public @NotNull InteractionEntity spawnInteractionEntity(@NotNull Vector3dc position) {
        Interaction entity = new Interaction(EntityType.INTERACTION, this.level);
        entity.setPos(position.x(), position.y(), position.z());
        this.level.addFreshEntity(entity);
        return new FabricInteractionEntity(entity);
    }

    @Override
    public @NotNull BlockState getBlock(@NotNull Vector3ic position) {
        return new FabricBlockState(this.level.getBlockState(new BlockPos(position.x(), position.y(), position.z())));
    }

    @Override
    public double getGroundHeight(double x, double z, double yTop, double yBottom) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        double localX = x - blockX;
        double localZ = z - blockZ;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int blockY = (int) Math.floor(yTop); blockY >= (int) Math.floor(yBottom); blockY--) {
            pos.set(blockX, blockY, blockZ);
            VoxelShape shape = this.level.getBlockState(pos).getCollisionShape(this.level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            // The height of the shape's column at the horizontal point. Negative
            // infinity if the shape has no geometry at this exact point (e.g. next
            // to a fence post).
            double top = shape.max(Direction.Axis.Y, localX, localZ);
            if (top == Double.NEGATIVE_INFINITY) {
                continue;
            }
            double surfaceY = blockY + top;
            if (surfaceY < yBottom) {
                return Double.NaN;
            }
            return Math.min(surfaceY, yTop);
        }
        return Double.NaN;
    }

    @Override
    public void spawnParticle(Vector3ic blockPos, Identifier identifier) {
        this.level.sendParticles(
            ParticleTypes.SMOKE,
            blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5,
            1, 0, 0, 0, 0);
    }
}
