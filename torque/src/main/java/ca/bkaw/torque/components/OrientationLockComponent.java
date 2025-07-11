package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class OrientationLockComponent implements VehicleComponent {
    public static VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "orientation_lock"),
        OrientationLockComponent::new
    );

    public OrientationLockComponent(Vehicle vehicle, DataInput dataInput) {

    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            Quaternionfc currentOrientation = rbc.getOrientation();

            Vector3f forward = new Vector3f(0, 0, -1).rotate(currentOrientation);
            float yaw = (float) Math.atan2(forward.x, forward.z);

            Quaternionf yawOnly = new Quaternionf().rotateY(yaw);

            float smoothingFactor = 0.1f;
            Quaternionf smoothed = new Quaternionf(currentOrientation).slerp(yawOnly, smoothingFactor);

            // rbc.setOrientation(smoothed); // TODO not the spirit
        });
    }
}
