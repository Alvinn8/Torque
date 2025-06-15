package ca.bkaw.torque;

import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.Vehicle;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/**
 * A handler for the {@code /torque} command. Note that the platform performs the
 * registration of the command and simply calls methods on this class.
 */
public class TorqueCommand {
    private final @NotNull Torque torque;

    public TorqueCommand(@NotNull Torque torque) {
        this.torque = torque;
    }

    public void summon(World world, Vector3d position) {
        VehicleModel model = new VehicleModel();
        Vehicle vehicle = new Vehicle(model);
        vehicle.addComponent(new RigidBodyComponent(1500, new Matrix3d(), world, position, new Quaterniond()));
        this.torque.getVehicleManager().addVehicle(vehicle);

        ItemDisplay itemDisplay = world.spawmItemDisplay(position);
        itemDisplay.setItem(this.torque.getPlatform().createModelItem(new Identifier("torque", "vehicle/car/primary")));
    }

    public void resourcePack(Player player) {
        this.torque.getAssets().getSender().send(player, true, "To see Torque vehicles.");
    }
}
