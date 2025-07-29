package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.World;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.joml.Vector3ic;

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

    @Override
    public boolean isCollidable(World world, Vector3ic position) {
        return !((PaperWorld) world).world().getBlockAt(position.x(), position.y(), position.z()).isPassable();
    }
}
