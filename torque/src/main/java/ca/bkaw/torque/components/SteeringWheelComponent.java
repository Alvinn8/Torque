package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.vehicle.PartRotationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class SteeringWheelComponent implements VehicleComponent, PartRotationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "steering_wheel"),
        SteeringWheelComponent::new
    );

    private float angle;

    public SteeringWheelComponent(Vehicle vehicle, DataInput dataInput) {
        this.angle = dataInput.readFloat("angle", 0.0f);
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {
        data.writeFloat("angle", this.angle);
    }

    @Override
    public void tick(Vehicle vehicle) {
        Input driverInput = vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);

        this.angle *= 0.9f;

        if (driverInput != null) {
            if (driverInput.left) {
                this.angle -= 0.1f;
            }
            if (driverInput.right) {
                this.angle += 0.1f;
            }
        }
    }

    @Override
    public Quaternionf getPartRotation(@NotNull String partName, @NotNull Vehicle vehicle) {
        // Check if this part name contains "steering_wheel" (case insensitive)
        if (partName.toLowerCase().contains("steering_wheel")) {
            // Rotate around Z-axis (negative Z is forward)
            return new Quaternionf().rotateZ(-this.angle);
        }
        return null; // This component doesn't control this part
    }

    /**
     * Get the current steering angle in radians.
     * 
     * @return The steering angle
     */
    public float getAngle() {
        return this.angle;
    }

    /**
     * Set the steering angle in radians.
     * 
     * @param angle The new steering angle
     */
    public void setAngle(float angle) {
        this.angle = angle;
    }
}
