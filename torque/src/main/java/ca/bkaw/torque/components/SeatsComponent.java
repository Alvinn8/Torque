package ca.bkaw.torque.components;

import ca.bkaw.torque.model.Seat;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.platform.Player;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * The component that keeps track of the passengers in the vehicle.
 */
public class SeatsComponent implements VehicleComponent {
    private final @NotNull Vehicle vehicle;
    private final Map<Seat, PassengerData> passengerData = new HashMap<>();

    public SeatsComponent(@NotNull Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public record PassengerData(Player passenger, long enteredAt, Input input) {
        public PassengerData(Player passenger) {
            this(passenger, System.currentTimeMillis(), new Input());
        }

        public void exitSeat() {
            this.passenger.dismountVehicle();
        }

        public boolean isValid() {
            return true;
            // return this.passenger.getMountedVehicle() != null;
        }
    }

    @Override
    public void tick() {
        var iter = this.passengerData.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            PassengerData passengerData = entry.getValue();
            // Update input for the passenger
            passengerData.passenger.getInput(passengerData.input);

            if (passengerData.input.shift) {
                // If the passenger is holding shift, exit the seat
                passengerData.exitSeat();
                iter.remove();
                VehicleManager vehicleManager = this.vehicle.getTorque().getVehicleManager();
                vehicleManager.setCurrentVehicle(passengerData.passenger, null);
            }
        }
    }

    /**
     * Get the raw passenger data map. Use the other methods instead when possible.
     *
     * @return The map of seats to passenger data.
     */
    public Map<Seat, PassengerData> getPassengerData() {
        return this.passengerData;
    }

    /**
     * Get the passenger in the given seat, or null if the seat is empty.
     *
     * @param seat The seat.
     * @return The passenger, or null.
     */
    @Nullable
    public Player getPassenger(Seat seat) {
        PassengerData passengerData = this.passengerData.get(seat);
        if (passengerData == null) return null;
        if (!passengerData.isValid()) {
            return null;
        }
        return passengerData.passenger();
    }

    /**
     * Get the seat that the specified passenger is currently sitting in. Or null
     * if none.
     *
     * @param passenger The passenger.
     * @return The seat the passenger is in. Or null if none.
     */
    @Nullable
    public Seat getPassengerSeat(Player passenger) {
        for (Map.Entry<Seat, PassengerData> entry : this.passengerData.entrySet()) {
            PassengerData passengerData = entry.getValue();
            if (passengerData.passenger().equals(passenger) && passengerData.isValid()) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the closest seat that is not already occupied.
     *
     * @param position The position in world coordinates.
     * @return The closest available seat, or null if no seats are available.
     */
    @Nullable
    public Seat getClosestAvailiableSeat(@NotNull Vector3dc position) {
        Vector3f positionF = new Vector3f(position);
        Seat closestSeat = null;
        double closestDistanceSq = Double.MAX_VALUE;

        RigidBodyComponent rbc = this.vehicle.getComponent(RigidBodyComponent.class).orElseThrow();
        Vector3f vehiclePosition = new Vector3f(rbc.getPosition());
        Quaternionfc vehicleOrientation = rbc.getOrientation();

        for (Seat seat : this.vehicle.getModel().getSeats()) {
            if (this.getPassenger(seat) != null) {
                continue; // Seat is already occupied
            }

            Vector3f seatPosition = vehiclePosition.add(
                seat.getTranslation().rotate(vehicleOrientation, new Vector3f())
            );
            double distanceSq = positionF.distanceSquared(seatPosition);

            if (distanceSq < closestDistanceSq) {
                closestDistanceSq = distanceSq;
                closestSeat = seat;
            }
        }

        return closestSeat;
    }

    /**
     * Set the passenger in a specific seat. If the seat is already occupied, the old
     * passenger will be exited.
     *
     * @param seat The seat.
     * @param passenger The passenger.
     */
    public void setPassenger(@NotNull Seat seat, @Nullable Player passenger) {
        VehicleManager vehicleManager = this.vehicle.getTorque().getVehicleManager();

        Seat oldSeat = this.getPassengerSeat(passenger);
        if (oldSeat != null) {
            this.passengerData.remove(oldSeat);
        }

        PassengerData oldPassengerData = this.passengerData.remove(seat);
        if (oldPassengerData != null) {
            oldPassengerData.exitSeat();
            vehicleManager.setCurrentVehicle(oldPassengerData.passenger(), null);
        }
        if (passenger != null) {
            this.passengerData.put(seat, new PassengerData(passenger));
            vehicleManager.setCurrentVehicle(passenger, this.vehicle);
        }
    }

    /**
     * Attempt to add the entity as a passenger in the vehicle
     *
     * @param passenger The passenger that wants to enter the vehicle
     * @return {@code true} if the passenger entered the vehicle, {@code false} if not
     */
    public boolean addPassenger(@NotNull Player passenger) {
        Seat seat = this.getClosestAvailiableSeat(passenger.getPosition());
        if (seat == null) return false;
        this.setPassenger(seat, passenger);
        return true;
    }

    /**
     * Get the input of the driver, or null if there is no driver.
     *
     * @return The input.
     */
    @Nullable
    public Input getDriverInput() {
        Input input = null;
        for (var entry : this.passengerData.entrySet()) {
            Seat seat = entry.getKey();
            PassengerData passengerData = entry.getValue();
            if (!seat.isDriver()) {
                continue;
            }
            if (input == null) {
                input = passengerData.input();
            } else {
                // Merge inputs
                input.merge(passengerData.input());
            }
        }
        return input;
    }
}
