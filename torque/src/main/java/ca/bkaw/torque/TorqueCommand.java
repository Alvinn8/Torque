package ca.bkaw.torque;

import ca.bkaw.torque.components.RigidBodyComponent;
import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.components.TestDriveComponent;
import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleManager;
import ca.bkaw.torque.vehicle.VehicleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

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
        VehicleType vehicleType = this.torque.getVehicleManager().getVehicleTypeRegistry().get(new Identifier("torque", "car"));
        if (vehicleType == null) {
            throw new IllegalArgumentException("Vehicle type not found.");
        }
        this.torque.getVehicleManager().spawnVehicle(vehicleType, world, position);
    }

    @Nullable
    private Vehicle getClosestVehicle(Player player) {
        Vector3d playerPosition = player.getPosition();
        return this.torque.getVehicleManager().getVehicles().stream().min((a, b) -> {
            Vector3dc aPos = a.getComponent(RigidBodyComponent.class).map(RigidBodyComponent::getPosition).orElse(new Vector3d());
            Vector3dc bPos = b.getComponent(RigidBodyComponent.class).map(RigidBodyComponent::getPosition).orElse(new Vector3d());
            double aDistance = aPos.distanceSquared(playerPosition);
            double bDistance = bPos.distanceSquared(playerPosition);
            return Double.compare(aDistance, bDistance);
        }).orElse(null);
    }

    public void test(Player player, int number) {
        switch (number) {
            case 1 -> {
                Vehicle vehicle = this.getClosestVehicle(player);
                if (vehicle != null) {
                    vehicle.getComponent(RigidBodyComponent.class).ifPresent(
                        rbc -> {
                            rbc.addForce(new Vector3d(0, 10000, 0), rbc.getPosition().add(10000, 0, 0, new Vector3d()));
                        }
                    );
                }
            }
            case 2 -> {
                Vehicle vehicle = this.getClosestVehicle(player);
                if (vehicle != null) {
                    vehicle.getComponent(SeatsComponent.class).ifPresent(seats -> {
                        boolean success = seats.addPassenger(player);
                        Debug.print("success = " + success);
                    });
                }
            }
            case 3 -> {
                try {
                    this.torque.getVehicleManager().saveAll();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                Debug.print("Saved all");
            }
            case 4 -> {
                Vehicle vehicle = this.getClosestVehicle(player);
                if (vehicle != null) {
                    vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
                        // Rotate 45 degrees around the Y axis.
                        ((Quaternionf) rbc.getOrientation()).rotateAxis((float) Math.PI / 4, 0, 1, 0); // 45 degrees in radians
                    });
                }
            }
            case 6 -> {
                Debug.setInstance(new Debug(this.torque));
            }
            case 7 -> {
                Vehicle vehicle = this.getClosestVehicle(player);
                if (vehicle != null) {
                    vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
                        World world = rbc.getWorld();
                        Debug.visualizeVectorAt(rbc.getWorld(), player.getPosition(), new Vector3d(0, 5, 0), "lime_wool");
                    });
                }
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

    /**
     * Freeze vehicle ticking.
     */
    public void tickFreeze() {
        this.torque.getVehicleManager().freezeTicking();
    }

    /**
     * Unfreeze vehicle ticking.
     */
    public void tickUnfreeze() {
        this.torque.getVehicleManager().unfreezeTicking();
    }

    /**
     * Step vehicle ticking by a certain number of ticks.
     *
     * @param steps The number of ticks to step (defaults to 1 if not specified).
     */
    public void tickStep(int steps) {
        this.torque.getVehicleManager().stepTicks(steps);
    }

    /**
     * Get information about the current tick state.
     *
     * @return A string describing the current tick state.
     */
    public String getTickStatus() {
        VehicleManager vm = this.torque.getVehicleManager();
        if (vm.isTickingFrozen()) {
            return "Ticking is frozen. Remaining steps: " + vm.getRemainingSteps();
        } else {
            return "Ticking is running normally.";
        }
    }
}
