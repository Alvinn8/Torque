package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.InteractionEntity;
import io.papermc.paper.entity.TeleportFlag;
import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.joml.Vector3dc;

public record PaperInteractionEntity(Interaction entity) implements InteractionEntity {

    @Override
    public void setPosition(Vector3dc position) {
        this.entity.teleport(
            new Location(this.entity.getWorld(), position.x(), position.y(), position.z()),
            TeleportFlag.EntityState.RETAIN_PASSENGERS
        );
    }

    @Override
    public void setSize(float width, float height) {
        this.entity.setInteractionWidth(width);
        this.entity.setInteractionHeight(height);
    }
}
