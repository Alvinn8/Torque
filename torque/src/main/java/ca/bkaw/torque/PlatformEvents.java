package ca.bkaw.torque;

import ca.bkaw.torque.components.SeatsComponent;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import ca.bkaw.torque.platform.entity.Player;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleManager;

/**
 * Methods called when certain events happen in the game.
 * <p>
 * These methods are called by the platform-specific implementation.
 */
public class PlatformEvents {
    private final Torque torque;

    public PlatformEvents(Torque torque) {
        this.torque = torque;
    }

    public void loadVehicleFromData(ItemDisplay primaryEntity) {
        this.torque.getVehicleManager().loadVehicle(primaryEntity);
    }

    public void onItemDisplaySave(ItemDisplay itemDisplay) {
        VehicleManager vehicleManager = this.torque.getVehicleManager();
        Vehicle vehicle = vehicleManager.getVehicleFromPart(itemDisplay);
        if (vehicle == null) {
            return;
        }
        vehicleManager.saveVehicle(vehicle);
    }

    public void leftClickVehicle(Vehicle vehicle, Player player) {
        if (player.isInCreativeMode()) {
            this.torque.getVehicleManager().destroyVehicle(vehicle);
        }
    }

    public void rightClickVehicle(Vehicle vehicle, Player player) {
        VehicleManager vehicleManager = this.torque.getVehicleManager();
        if (vehicleManager.getCurrentVehicle(player) != null) {
            return;
        }
        vehicle.getComponent(SeatsComponent.class)
            .ifPresent(seats -> seats.addPassenger(player));
    }
}
