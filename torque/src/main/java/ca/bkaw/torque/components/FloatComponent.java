package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.BlockState;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class FloatComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "float"),
        FloatComponent::new
    );

    public FloatComponent(Vehicle vehicle, DataInput dataInput) {

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
            BlockState block = rbc.getWorld().getBlock(new Vector3i(rbc.getPosition(), RoundingMode.TRUNCATE));
            if (block.isWaterlogged()) {
                // Add buoyant force
                rbc.addForce(new Vector3d(0, 1000, 0), rbc.getPosition());
            }
        });
    }
}
