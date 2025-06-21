package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.joml.Vector3d;

public class TestDriveComponent implements VehicleComponent {
    public  static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "test_drive"),
        TestDriveComponent::new
    );

    private final Vehicle vehicle;

    public TestDriveComponent(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    @Override
    public void tick() {
        Input driverInput = this.vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);

        if (driverInput == null) {
            return;
        }

        this.vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            final double magnitude = 10000; // unit: Newton
            if (driverInput.forward) {
                rbc.addForce(new Vector3d(0, 0, -magnitude), new Vector3d());
            }
            if (driverInput.backward) {
                rbc.addForce(new Vector3d(0, 0, magnitude), new Vector3d());
            }
            if (driverInput.left) {
                rbc.addForce(new Vector3d(-magnitude, 0, 0), new Vector3d());
            }
            if (driverInput.right) {
                rbc.addForce(new Vector3d(magnitude, 0, 0), new Vector3d());
            }
        });
    }
}
