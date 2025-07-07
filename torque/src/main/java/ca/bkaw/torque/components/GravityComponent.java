package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class GravityComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "gravity"),
        GravityComponent::new
    );
    public static final double GRAVITATIONAL_ACCELERATION = 9.8; // unit: m/s²

    public GravityComponent(Vehicle vehicle, DataInput dataInput) {}

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            // Apply a downward force to simulate gravity
            double magnitude = -GRAVITATIONAL_ACCELERATION * vehicle.getType().mass();
            // rbc.addForce(new Vector3d(0, magnitude, 0), rbc.getPosition());
            double spreadDistance = 1;
            double yOffset = 0;
            rbc.addForce(new Vector3d(0, magnitude / 4, 0), rbc.getPosition().add(new Vector3d(spreadDistance, yOffset, 0).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
            rbc.addForce(new Vector3d(0, magnitude / 4, 0), rbc.getPosition().add(new Vector3d(-spreadDistance, yOffset, 0).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
            rbc.addForce(new Vector3d(0, magnitude / 4, 0), rbc.getPosition().add(new Vector3d(0, yOffset, spreadDistance).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
            rbc.addForce(new Vector3d(0, magnitude / 4, 0), rbc.getPosition().add(new Vector3d(0, yOffset, -spreadDistance).rotate(new Quaterniond(rbc.getOrientation())), new Vector3d()));
        });
    }
}
