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
import org.joml.Vector3d;
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
            Vector3d angularVelocity = rbc.getAngularVelocity();

            float smoothingFactor = 0.3f;

            // Only keep the component of angular velocity around the Y axis and apply smoothing
            Vector3d direction = new Vector3d(0, 1, 0); // Y axis
            double projection = angularVelocity.dot(direction) / direction.lengthSquared();
            Vector3d filteredAngularVelocity = new Vector3d(direction).mul(projection);
            Vector3d smoothedAngularVelocity = new Vector3d(angularVelocity).lerp(filteredAngularVelocity, smoothingFactor);
            rbc.setAngularVelocity(smoothedAngularVelocity);

            // Swing-Twist decomposition to isolate yaw (twist around Y axis)
            Vector3f directionF = new Vector3f(0, 1, 0);
            Vector3f ra = new Vector3f(currentOrientation.x(), currentOrientation.y(), currentOrientation.z());
            Vector3f p = new Vector3f(directionF).mul(ra.dot(directionF) / directionF.lengthSquared());
            Quaternionf twist = new Quaternionf(p.x, p.y, p.z, currentOrientation.w()).normalize();

            Quaternionf smoothed = new Quaternionf(currentOrientation).slerp(twist, smoothingFactor);

            rbc.setOrientation(smoothed); // TODO not the spirit
        });
    }
}
