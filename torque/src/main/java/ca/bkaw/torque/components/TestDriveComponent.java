package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class TestDriveComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "test_drive"),
        TestDriveComponent::new
    );

    private final Vehicle vehicle;

    public TestDriveComponent(Vehicle vehicle, DataInput data) {
        this.vehicle = vehicle;
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {

    }

    @Override
    public void tick(Vehicle vehicle) {
        Input driverInput = this.vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);

        if (driverInput == null) {
            return;
        }

        this.vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            Vector3dc position = rbc.getPosition();
            double magnitude = 10_000; // unit: Newton
            if (driverInput.sprint) {
                magnitude *= 10;
            }
            if (driverInput.forward && false) {
                rbc.addForce(new Vector3d(0, 0, -magnitude).rotate(new Quaterniond(rbc.getOrientation())), position);
            }
            if (driverInput.backward && false) {
                rbc.addForce(new Vector3d(0, 0, magnitude).rotate(new Quaterniond(rbc.getOrientation())), position);
            }
            if (false) {
                if (driverInput.right) {
                    rbc.addForce(new Vector3d(magnitude, 0, 0).rotate(new Quaterniond(rbc.getOrientation())), position);
                }
                if (driverInput.left) {
                    rbc.addForce(new Vector3d(-magnitude, 0, 0).rotate(new Quaterniond(rbc.getOrientation())), position);
                }
                return;
            }

            // Steering by applying lateral forces at front and rear
            double steerForce = 5_000; // Adjust for steering sensitivity
            double steerOffset = 2.0;  // Distance from center of mass

            if (driverInput.left && false) {
                // Rightward force at front, leftward at rear
                Vector3d front = new Vector3d(0, 0, steerOffset).rotate(new Quaterniond(rbc.getOrientation())).add(position, new Vector3d());
                Vector3d rear = new Vector3d(0, 0, -steerOffset).rotate(new Quaterniond(rbc.getOrientation())).add(position, new Vector3d());
                Vector3d right = new Vector3d(1, 0, 0).rotate(new Quaterniond(rbc.getOrientation()));
                Vector3d left = new Vector3d(-1, 0, 0).rotate(new Quaterniond(rbc.getOrientation()));
                rbc.addForce(right.mul(steerForce, new Vector3d()), front);
                rbc.addForce(left.mul(steerForce, new Vector3d()), rear);
            }
            if (driverInput.right && false) {
                // Leftward force at front, rightward at rear
                Vector3d front = new Vector3d(0, 0, steerOffset).rotate(new Quaterniond(rbc.getOrientation())).add(position, new Vector3d());
                Vector3d rear = new Vector3d(0, 0, -steerOffset).rotate(new Quaterniond(rbc.getOrientation())).add(position, new Vector3d());
                Vector3d right = new Vector3d(1, 0, 0).rotate(new Quaterniond(rbc.getOrientation()));
                Vector3d left = new Vector3d(-1, 0, 0).rotate(new Quaterniond(rbc.getOrientation()));
                rbc.addForce(left.mul(steerForce, new Vector3d()), front);
                rbc.addForce(right.mul(steerForce, new Vector3d()), rear);
            }
        });
    }
}
