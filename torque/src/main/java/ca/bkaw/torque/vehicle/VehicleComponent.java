package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.platform.DataOutput;

public interface VehicleComponent {

    /**
     * Get the {@link VehicleComponentType type} of this component.
     *
     * @return The type of this component.
     */
    VehicleComponentType getType();

    /**
     * Serialize component state to the given data.
     * <p>
     * The component gets its own data section in the given data so name conflicts do
     * not occur.
     *
     * @param vehicle The vehicle this component belongs to.
     * @param data The given data where the component state should be saved.
     */
    void save(Vehicle vehicle, DataOutput data);

    /**
     * Called each tick when the vehicle is being simulated.
     *
     * @param vehicle The vehicle this component belongs to.
     */
    void tick(Vehicle vehicle);
}
