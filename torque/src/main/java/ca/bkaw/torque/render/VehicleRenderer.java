package ca.bkaw.torque.render;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.model.Seat;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class VehicleRenderer {
    private @NotNull Vehicle vehicle;
    private @NotNull RenderEntity primary;

    public record RenderEntity(@NotNull ItemDisplay display, @NotNull Matrix4f transformation) {}

    public VehicleRenderer(@NotNull Vehicle vehicle) {
        this.vehicle = vehicle;
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        this.primary = new RenderEntity(
            rigidBody.getWorld().spawnItemDisplay(rigidBody.getPosition()),
            new Matrix4f()
        );
    }

    public void setup(@NotNull Torque torque) {
        this.primary.display.setItem(torque.getPlatform().createModelItem(new Identifier("torque", "vehicle/car/primary")));
        this.render();
    }

    public void render() {
        RigidBodyComponent rigidBody = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();

        Seat seat = this.vehicle.getModel().getSeats().getFirst();

        Matrix4f transformation = this.primary.transformation;
        Quaternionf quaternion = rigidBody.getOrientation();
        transformation.identity()
            .rotate(new Quaternionf().rotateAxis((float) Math.PI, 0, 1, 0))
            .rotate(quaternion)
            .translate(seat.getTranslation())
            .translate(this.vehicle.getModel().getTranslation())
            .scale((float) this.vehicle.getModel().getScale())
            .translate(0.0f, 0.5f, 0.0f)
        ;
        this.primary.display.setTransformation(transformation);
        this.primary.display.setPosition(rigidBody.getPosition().sub(seat.getTranslation().rotate(quaternion, new Vector3f()), new Vector3d()));
        this.primary.display.setStartInterpolation(0);
    }

    public Object getVehicle() {
        return this.vehicle;
    }

}
