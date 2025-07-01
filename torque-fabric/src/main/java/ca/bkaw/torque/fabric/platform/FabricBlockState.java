package ca.bkaw.torque.fabric.platform;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public record FabricBlockState(net.minecraft.world.level.block.state.BlockState blockState) implements ca.bkaw.torque.platform.BlockState {
    @Override
    public boolean isWaterlogged() {
        if (this.blockState.getBlock() == Blocks.WATER) {
            return true;
        }
        return this.blockState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);
    }
}
