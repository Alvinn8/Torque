package ca.bkaw.torque.render;

import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class VehicleRenderer {
    private @NotNull Vehicle vehicle;
    private @NotNull RenderEntity primary;

    public record RenderEntity(@NotNull ItemDisplay display, @NotNull Matrix4f transformation) {}

    public VehicleRenderer(@NotNull Vehicle vehicle) {
        this.vehicle = vehicle;
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        Matrix4f transformation = new Matrix4f().identity().translate(0, 5, 0).scale(10);
        this.primary = new RenderEntity(
            rigidBody.getWorld().spawnItemDisplay(rigidBody.getPosition(), transformation),
            transformation
        );
    }

    public void render() {
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        Matrix4f transformation = this.primary.transformation;
        transformation.identity()
            .rotate(rigidBody.getOrientation())
            .translate(0.0f, 0.5f, 0.0f)
            .scale(10);
        this.primary.display.setTransformation(transformation);
        this.primary.display.setPosition(rigidBody.getPosition());
    }
}
