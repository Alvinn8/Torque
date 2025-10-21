package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.tags.WheelTags;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.List;

/**
 * Example component that controls wheel rotation based on vehicle movement.
 */
public class WheelComponent implements VehicleComponent, PartTransformationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.builder(
        new Identifier("torque", "wheel")
    ).create(WheelComponent::new);

    /**
     * The ratio between how much the wheels turn for each degree of the steering wheel.
     */
    public static final float STEERING_WHEEL_RATIO = 1f / 10f;

    private static final class WheelData {
        private final WheelTags.Wheel wheel;
        private float rotation; // unit: rad
        private float speed; // unit: rad/s
        private double steerAngle; // unit: rad

        public WheelData(WheelTags.Wheel wheel, float rotation, float speed) {
            this.wheel = wheel;
            this.rotation = rotation;
            this.speed = speed;
        }
    }

    private final List<WheelData> wheels = new ArrayList<>();
    private final double wheelbase;
    private final double backAxleOffset;
    private final int numberOfDrivenWheels;

    public WheelComponent(Vehicle vehicle, JsonObject config, DataInput dataInput) {
        vehicle.getType().model().getTagData(WheelTags.class)
            .ifPresent(wheels -> wheels.forEach(wheel ->
                this.wheels.add(new WheelData(wheel, 0, 0))
            ));
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        int numberOfDrivenWheels = 0;
        for (WheelData wheel : this.wheels) {
            double z = wheel.wheel.contactPatch().z;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
            if (wheel.wheel.driven()) {
                numberOfDrivenWheels++;
            }
        }
        this.wheelbase = maxZ - minZ;
        this.backAxleOffset = maxZ; // In model coordinates +Z is backwards
        this.numberOfDrivenWheels = numberOfDrivenWheels;
        Debug.print("wheelbase = " + this.wheelbase + ", backAxleOffset = " + this.backAxleOffset + ", numberOfDrivenWheels = " + this.numberOfDrivenWheels);
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        RigidBodyComponent rbc = vehicle.getComponent(RigidBodyComponent.class).orElse(null);
        if (rbc == null) {
            return;
        }
        Vector3dc vehiclePosition = rbc.getPosition();
        Vector3dc vehicleVelocity = rbc.getVelocity();
        Vector3dc vehicleAngularVelocity = rbc.getAngularVelocity();
        Quaterniondc orientation = new Quaterniond(rbc.getOrientation());

        Input driverInput = vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);

        double driveMagnitude = 0;
        if (driverInput != null) {
            if (driverInput.forward) {
                driveMagnitude = 10_000.0;
            } else if (driverInput.backward) {
                driveMagnitude = -5_000.0;
            }
        }

        Vector3d vehicleForward = new Vector3d(0, 0, -1).rotate(orientation);
        Vector3d vehicleUp = new Vector3d(0, 1, 0).rotate(orientation);

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);

        // Ackermann steering geometry
        float averageSteeringAngle = steeringWheelAngle * STEERING_WHEEL_RATIO;
        double turningRadius;
        if (Math.abs(averageSteeringAngle) < 1e-6) {
            turningRadius = Double.POSITIVE_INFINITY;
        } else {
            turningRadius = this.wheelbase * Math.tan(Math.PI / 2 - averageSteeringAngle);
        }
        if (Double.isFinite(turningRadius) && Math.abs(turningRadius) < 30) {
            Debug.highlightPositionSmall(rbc.getWorld(), new Vector3d(turningRadius, 0, this.backAxleOffset).rotate(orientation).add(vehiclePosition), "blue_wool");
        }

        for (WheelData wheel : this.wheels) {
            // Calculate direction vectors and the contact patch position.
            Vector3dc wheelForward = vehicleForward;
            if (wheel.wheel.steerable()) {
                // Ackermann steering geometry
                // Calculate the angle from wheel to the rotation center
                Vector3d cp = wheel.wheel.contactPatch();
                // delta y = distance between wheel and back axle
                // delta x = distance between wheel and turning circle
                // tan(steerAngle) = delta y / delta x
                // => steerAngle = arctan(delta y / delta x)
                wheel.steerAngle = Math.atan((this.backAxleOffset - cp.z) / (turningRadius - cp.x));
                wheelForward = new Vector3d(vehicleForward).rotateY(-wheel.steerAngle);
            }

            Vector3d wheelRight = new Vector3d(vehicleUp).cross(wheelForward);

            Vector3d worldContactPatch = new Vector3d(wheel.wheel.contactPatch())
                .add(vehicle.getType().model().getPrimary().translation())
                .rotate(orientation)
                .add(vehiclePosition);

            // Apply driving force from engine to driven wheels
            if (wheel.wheel.driven()) {
                Vector3d drivingForce = new Vector3d(wheelForward).mul(driveMagnitude / this.numberOfDrivenWheels);
                rbc.addForce(drivingForce, worldContactPatch);
                Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(drivingForce).div(1000), "red_wool");
            }

            // Update visual wheel rotation
            wheel.rotation += wheel.speed * (float) RigidBodyComponent.DELTA_TIME;

            // Get the local velocity of the wheel, with the effect of angular velocity
            Vector3d velocity = new Vector3d(vehicleAngularVelocity).cross(new Vector3d(worldContactPatch).sub(vehiclePosition)).add(vehicleVelocity);

            // Cornering force (lateral force)
            // Firstly, we need to calculate the slip angle.
            double forwardSpeed = velocity.dot(wheelForward);
            double lateralSpeed = velocity.dot(wheelRight);

            double slipAngle = Math.atan2(lateralSpeed, Math.abs(forwardSpeed) + 0.01);

            // At low velocities, limit the slip angle to avoid oscillations.
            slipAngle *= Math.min(1.0, Math.abs(forwardSpeed) / 3.0);

            // Simple formula with a hard-coded cornering stiffness
            double lateralForceMagnitude = -slipAngle * 15000;

            Vector3d lateralForce = new Vector3d(wheelRight).mul(lateralForceMagnitude);
            rbc.addForce(lateralForce, worldContactPatch);

            Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(lateralForce).div(1000), "pink_wool");
        }
    }

    @Override
    public PartTransform getPartTransform(@NotNull String partName, @Nullable Object partData, @NotNull Vehicle vehicle) {
        if (!(partData instanceof WheelTags.Wheel wheel)) {
            return null;
        }

        Quaternionf rotation = new Quaternionf();

        for (WheelData wheelData : this.wheels) {
            if (wheelData.wheel == wheel) {
                if (wheel.steerable()) {
                    rotation.rotateY((float) -wheelData.steerAngle);
                }
                rotation.rotateX(-wheelData.rotation);
                break;
            }
        }

        return new PartTransform(rotation);
    }

}
