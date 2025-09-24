package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.entity.InteractionEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import org.joml.Vector3dc;

public record FabricInteractionEntity(Interaction entity) implements InteractionEntity {
    @Override
    public void setPosition(Vector3dc position) {
        this.entity.setPos(position.x(), position.y(), position.z());
    }

    @Override
    public void setSize(float width, float height) {
        this.entity.setWidth(width);
        this.entity.setHeight(height);
    }

    @Override
    public void remove() {
        this.entity.remove(Entity.RemovalReason.KILLED);
    }
}
