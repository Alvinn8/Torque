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
                magnitude *= 10; // double the force when sprinting
            }
            if (driverInput.forward) {
                rbc.addForce(new Vector3d(0, 0, -magnitude).rotate(new Quaterniond(rbc.getOrientation())), position);
            }
            if (driverInput.backward) {
                rbc.addForce(new Vector3d(0, 0, magnitude).rotate(new Quaterniond(rbc.getOrientation())), position);
            }
            if (driverInput.left) {
                rbc.addForce(new Vector3d(magnitude, 0, 0), position.add(new Vector3d(0, 0, 2).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
                rbc.addForce(new Vector3d(-magnitude, 0, 0), position.add(new Vector3d(0, 0, -2).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
            }
            double rotationMagnitude = 1_000;
            if (driverInput.right) {
                rbc.addForce(new Vector3d(rotationMagnitude, 0, 0), position.add(
                    new Vector3d(0, 0, -2).rotate(new Quaterniond(rbc.getOrientation())),
                    new Vector3d()
                ));
                rbc.addForce(new Vector3d(-rotationMagnitude, 0, 0), position.add(
                    new Vector3d(0, 0, 2).rotate(new Quaterniond(rbc.getOrientation())),
                    new Vector3d()
                ));
            }
        });
    }
}
