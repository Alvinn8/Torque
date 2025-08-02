package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/**
 * Example component that controls wheel rotation based on vehicle movement.
 */
public class WheelComponent implements VehicleComponent, PartTransformationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "wheel"),
        WheelComponent::new
    );

    private float wheelRotation;
    private float wheelSpeed;

    public WheelComponent(Vehicle vehicle, DataInput dataInput) {
        this.wheelRotation = dataInput.readFloat("rotation", 0.0f);
        this.wheelSpeed = dataInput.readFloat("speed", 0.0f);
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {
        data.writeFloat("rotation", this.wheelRotation);
        data.writeFloat("speed", this.wheelSpeed);
    }

    @Override
    public void tick(Vehicle vehicle) {
        // Get vehicle velocity and calculate wheel rotation
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rigidBody -> {
            // Simple wheel rotation based on forward velocity
            // This is a simplified calculation - in reality you'd want to consider
            // wheel circumference, gear ratios, etc.
            float forwardVelocity = (float) rigidBody.getVelocity().length();
            this.wheelSpeed = forwardVelocity * 0.5f;
            this.wheelRotation += this.wheelSpeed * (float) RigidBodyComponent.DELTA_TIME;
        });
    }

    @Override
    public PartTransform getPartTransform(@NotNull String partName, @NotNull Vehicle vehicle) {
        if (!partName.startsWith("wheel")) {
            return null;
        }

        Quaternionf rotation = new Quaternionf();

        // First apply steering rotation around Y-axis (vertical steering)
        vehicle.getComponent(SteeringWheelComponent.class).ifPresent(steeringWheel -> {
            float steeringAngle = steeringWheel.getAngle() * 0.8f;
            rotation.rotateY(-steeringAngle);
        });

        // Then apply wheel spinning rotation around X-axis
        rotation.rotateX(-this.wheelRotation);

        return new PartTransform(rotation);
    }

    /**
     * Get the current wheel rotation in radians.
     * 
     * @return The wheel rotation
     */
    public float getWheelRotation() {
        return this.wheelRotation;
    }

    /**
     * Get the current wheel speed.
     * 
     * @return The wheel speed
     */
    public float getWheelSpeed() {
        return this.wheelSpeed;
    }
}
