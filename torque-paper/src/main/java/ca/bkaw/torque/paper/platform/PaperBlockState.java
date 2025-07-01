package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

public record PaperBlockState(BlockData blockData) implements BlockState {
    @Override
    public boolean isWaterlogged() {
        if (this.blockData.getMaterial() == Material.WATER) {
            return true;
        }
        if (this.blockData instanceof Waterlogged waterlogged) {
            return waterlogged.isWaterlogged();
        }
        return false;
    }
}
