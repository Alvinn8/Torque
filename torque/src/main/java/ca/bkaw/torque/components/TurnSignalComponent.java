package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class TurnSignalComponent implements VehicleComponent, PartTransformationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "turn_signal"),
        TurnSignalComponent::new
    );

    public static int YELLOW = 0xFFFF00;

    private boolean left = true;
    private boolean right = false;
    private int timeTicks = 0;

    public TurnSignalComponent(Vehicle vehicle, DataInput dataInput) {}

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        this.timeTicks++;
    }

    @Override
    public @Nullable PartTransform getPartTransform(@NotNull String partName, @NotNull Vehicle vehicle) {
        boolean glow = this.timeTicks % 20 < 10; // Toggle every second
        if (partName.equals("light_turn_signal_left")) {
            return new PartTransform(new Quaternionf(), new Vector3f(), this.left && glow, YELLOW);
        } else if (partName.equals("light_turn_signal_right")) {
            return new PartTransform(new Quaternionf(), new Vector3f(), this.right && glow, YELLOW);
        }
        return null;
    }
}
