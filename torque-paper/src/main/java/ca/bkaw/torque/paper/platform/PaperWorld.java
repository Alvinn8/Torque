package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.ItemDisplay;
import org.bukkit.Location;
import org.joml.Vector3d;

public record PaperWorld(org.bukkit.World world) implements ca.bkaw.torque.platform.World {
    @Override
    public ItemDisplay spawmItemDisplay(Vector3d position) {
        Location location = new Location(this.world, position.x(), position.y(), position.z());
        return new PaperItemDisplay(this.world.spawn(location, org.bukkit.entity.ItemDisplay.class));
    }
}
