package ca.bkaw.torque;

import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.components.TestDriveComponent;
import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
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
        VehicleType car = this.torque.getVehicleManager().getVehicleTypeRegistry().get(new Identifier("torque", "car"));
        if (car == null) {
            throw new IllegalArgumentException("Vehicle type 'torque:car' not found.");
        }
        this.torque.getVehicleManager().spawnVehicle(car, world, position);
    }

    public void test(Player player, int number) {
        switch (number) {
            case 1 -> {
                Vehicle vehicle = this.torque.getVehicleManager().getVehicles().get(0);
                if (vehicle != null) {
                    vehicle.getComponent(RigidBodyComponent.class).ifPresent(
                        rbc -> {
                            rbc.addForce(new Vector3d(1000, 0, 0), rbc.getPosition().add(1, 0, 0, new Vector3d()));
                        }
                    );
                }
            }
            case 2 -> {
                Vehicle vehicle = this.torque.getVehicleManager().getVehicles().get(0);
                if (vehicle != null) {
                    vehicle.getComponent(SeatsComponent.class).ifPresent(seats -> {
                        boolean success = seats.addPassenger(player);
                        System.out.println("success = " + success);
                    });
                }
            }
            case 3 -> {
                try {
                    this.torque.getVehicleManager().saveAll();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                System.out.println("Saved all");
            }
        }
    }

    public void resourcePack(Player player) {
        this.torque.getAssets().getSender().send(player, true, "To see Torque vehicles.");
    }

    public void reload() {
        try {
            this.torque.reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
