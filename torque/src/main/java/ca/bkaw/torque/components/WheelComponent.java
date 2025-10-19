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

        public WheelData(WheelTags.Wheel wheel, float rotation, float speed) {
            this.wheel = wheel;
            this.rotation = rotation;
            this.speed = speed;
        }
    }

    private final List<WheelData> wheels = new ArrayList<>();

    public WheelComponent(Vehicle vehicle, JsonObject config, DataInput dataInput) {
        vehicle.getType().model().getTagData(WheelTags.class)
            .ifPresent(wheels -> wheels.forEach(wheel ->
                this.wheels.add(new WheelData(wheel, 0, 0))
            ));
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
        Vector3d vehicleVelocity = rbc.getVelocity();
        Quaterniond orientation = new Quaterniond(rbc.getOrientation());

        Vector3d forward = new Vector3d(0, 0, 1).rotate(orientation);
        Vector3d right = new Vector3d(1, 0, 0).rotate(orientation);

        double forwardSpeed = vehicleVelocity.dot(forward);
        double lateralSpeed = vehicleVelocity.dot(right);

        double defaultSlipAngle = Math.atan2(lateralSpeed, Math.abs(forwardSpeed) + 0.01);

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);

        float steeringAngle = steeringWheelAngle * 0.8f;

        boolean print = Math.random() < 0.01;
        for (WheelData wheel : this.wheels) {
            wheel.rotation += wheel.speed * (float) RigidBodyComponent.DELTA_TIME;
            double wheelDeltaAngle = wheel.wheel.steerable() ? steeringAngle : 0.0f;

            double slipAngle = defaultSlipAngle - wheelDeltaAngle;
            if (print) Debug.print("slipAngle = " + slipAngle);
            double lateralForceMagnitude = -slipAngle * 15000;

            Vector3d lateralForce = new Vector3d(right).mul(lateralForceMagnitude);
            Vector3d worldContactPatch = new Vector3d(wheel.wheel.contactPatch())
                .add(vehicle.getType().model().getPrimary().translation())
                .rotate(orientation)
                .add(vehiclePosition);
            rbc.addForce(lateralForce, worldContactPatch);
            // Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(lateralForce).div(1000), "pink_wool");
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

        if (wheel.steerable()) {
            // First, apply steering rotation around Y-axis (vertical steering)
            vehicle.getComponent(SteeringWheelComponent.class).ifPresent(steeringWheel -> {
                float steeringAngle = steeringWheel.getAngle() * 0.8f;
                rotation.rotateY(-steeringAngle);
            });
        }

        // Then apply wheel spinning rotation around X-axis
        for (WheelData wheelData : this.wheels) {
            if (wheelData.wheel == wheel) {
                rotation.rotateX(-wheelData.rotation);
                break;
            }
        }

        return new PartTransform(rotation);
    }

}
