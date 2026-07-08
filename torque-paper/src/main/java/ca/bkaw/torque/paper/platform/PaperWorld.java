package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.InteractionEntity;
import ca.bkaw.torque.platform.ItemDisplay;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.BoundingBox;
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
    public @NotNull InteractionEntity spawnInteractionEntity(@NotNull Vector3dc position) {
        Location location = new Location(this.world, position.x(), position.y(), position.z());
        return new PaperInteractionEntity(this.world.spawn(location, org.bukkit.entity.Interaction.class));
    }

    @Override
    public @NotNull BlockState getBlock(@NotNull Vector3ic position) {
        return new PaperBlockState(this.world.getBlockData(position.x(), position.y(), position.z()));
    }

    @Override
    public double getGroundHeight(double x, double z, double yTop, double yBottom) {
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        double localX = x - blockX;
        double localZ = z - blockZ;
        for (int blockY = (int) Math.floor(yTop); blockY >= (int) Math.floor(yBottom); blockY--) {
            // The height of the block's collision shape at the horizontal point.
            // The bounding boxes are relative to the block position.
            double top = Double.NEGATIVE_INFINITY;
            for (BoundingBox box : this.world.getBlockAt(blockX, blockY, blockZ).getCollisionShape().getBoundingBoxes()) {
                if (localX >= box.getMinX() && localX <= box.getMaxX()
                    && localZ >= box.getMinZ() && localZ <= box.getMaxZ()) {
                    top = Math.max(top, box.getMaxY());
                }
            }
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
        this.world.spawnParticle(Particle.SMOKE, blockPos.x(), blockPos.y(), blockPos.z(), 1);
    }
}
