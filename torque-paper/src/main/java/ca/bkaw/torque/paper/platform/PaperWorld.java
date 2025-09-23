package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.InteractionEntity;
import ca.bkaw.torque.platform.ItemDisplay;
import org.bukkit.Location;
import org.bukkit.Particle;
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
    public void spawnParticle(Vector3ic blockPos, Identifier identifier) {
        this.world.spawnParticle(Particle.SMOKE, blockPos.x(), blockPos.y(), blockPos.z(), 1);
    }
}
