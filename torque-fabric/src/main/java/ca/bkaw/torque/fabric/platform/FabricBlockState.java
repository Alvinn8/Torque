package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public record FabricBlockState(net.minecraft.world.level.block.state.BlockState blockState) implements ca.bkaw.torque.platform.BlockState {
    @Override
    public boolean isWaterlogged() {
        if (this.blockState.getBlock() == Blocks.WATER) {
            return true;
        }
        return this.blockState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);
    }

    @Override
    public boolean isCollidable(World world, Vector3ic position) {
        return !this.blockState.getCollisionShape(
            ((FabricWorld) world).level(),
            new BlockPos(position.x(), position.y(), position.z())
        ).isEmpty();
    }
}
