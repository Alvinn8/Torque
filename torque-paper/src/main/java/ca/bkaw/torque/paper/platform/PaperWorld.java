package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.ItemDisplay;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

public record PaperWorld(org.bukkit.World world) implements ca.bkaw.torque.platform.World {
    @Override
    @NotNull
    public ItemDisplay spawnItemDisplay(@NotNull Vector3dc position) {
        Location location = new Location(this.world, position.x(), position.y(), position.z());
        return new PaperItemDisplay(this.world.spawn(location, org.bukkit.entity.ItemDisplay.class));
    }

    @Override
    public @NotNull BlockState getBlock(@NotNull Vector3ic position) {
        return new PaperBlockState(this.world.getBlockData(position.x(), position.y(), position.z()));
    }
}
