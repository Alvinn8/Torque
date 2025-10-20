package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
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

    public WheelComponent(Vehicle vehicle, JsonObject config, DataInput dataInput) {
        vehicle.getType().model().getTagData(WheelTags.class)
            .ifPresent(wheels -> wheels.forEach(wheel ->
                this.wheels.add(new WheelData(wheel, 0, 0))
            ));
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (WheelData wheel : this.wheels) {
            double z = wheel.wheel.contactPatch().z;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
        }
        this.wheelbase = maxZ - minZ;
        this.backAxleOffset = maxZ; // In model coordinates +Z is backwards
        Debug.print("wheelbase = " + this.wheelbase + ", backAxleOffset = " + this.backAxleOffset);
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
        boolean print = Math.random() < 0.00;
        Vector3dc vehiclePosition = rbc.getPosition();
        Vector3d vehicleVelocity = rbc.getVelocity();
        Quaterniond orientation = new Quaterniond(rbc.getOrientation());

        Vector3d vehicleForward = new Vector3d(0, 0, 1).rotate(orientation);
        Vector3d vehicleUp = new Vector3d(0, 1, 0).rotate(orientation);

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);

        // Ackermann steering geometry
        float averageSteeringAngle = steeringWheelAngle * 0.8f;

        double turningRadius;
        if (Math.abs(averageSteeringAngle) < 1e-6) {
            turningRadius = Double.POSITIVE_INFINITY;
        } else {
            turningRadius = this.wheelbase * Math.tan(Math.PI / 2 - averageSteeringAngle);
        }
        if (print) Debug.print("turningRadius = " + turningRadius);
        if (Double.isFinite(turningRadius) && Math.abs(turningRadius) < 30) Debug.highlightPositionSmall(rbc.getWorld(), new Vector3d(turningRadius, 0, this.backAxleOffset).rotate(orientation).add(vehiclePosition), "blue_wool");

        for (WheelData wheel : this.wheels) {
            wheel.rotation += wheel.speed * (float) RigidBodyComponent.DELTA_TIME;
            Vector3dc wheelForward = vehicleForward;
            if (wheel.wheel.steerable()) {
                // Calculate angle from wheel to the instantaneous center of rotation
                Vector3d cp = wheel.wheel.contactPatch();
                wheel.steerAngle = Math.PI / 2 - Math.atan2(turningRadius - cp.x, this.backAxleOffset - cp.z);
                wheelForward = new Vector3d(vehicleForward).rotateY(-wheel.steerAngle);
            }

            Vector3d wheelRight = new Vector3d(vehicleUp).cross(wheelForward);

            double forwardSpeed = vehicleVelocity.dot(wheelForward);
            double lateralSpeed = vehicleVelocity.dot(wheelRight);

            double slipAngle = Math.atan2(lateralSpeed, Math.abs(forwardSpeed) + 0.01);
            slipAngle *= Math.min(1.0, Math.abs(forwardSpeed) / 3.0);
            if (print) Debug.print("slipAngle = " + slipAngle);
            double lateralForceMagnitude = -slipAngle * 15000;

            Vector3d lateralForce = new Vector3d(wheelRight).mul(lateralForceMagnitude);
            Vector3d worldContactPatch = new Vector3d(wheel.wheel.contactPatch())
                .add(vehicle.getType().model().getPrimary().translation())
                .rotate(orientation)
                .add(vehiclePosition);
            rbc.addForce(lateralForce, worldContactPatch);
            Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(lateralForce).div(1000), "pink_wool");
            // Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(wheelRight).mul(10), "white_wool");
            Debug.highlightPositionSmall(rbc.getWorld(), worldContactPatch, wheel.wheel.steerable() ? "purple_wool" : "yellow_wool");
        }
        if (print) Debug.print("---");
    }

    @Override
    public PartTransform getPartTransform(@NotNull String partName, @Nullable Object partData, @NotNull Vehicle vehicle) {
        if (!(partData instanceof WheelTags.Wheel wheel)) {
            return null;
        }

        Quaternionf rotation = new Quaternionf();

        // Then apply wheel spinning rotation around X-axis
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
