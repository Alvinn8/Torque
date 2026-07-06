package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.platform.DataOutput;
import org.jetbrains.annotations.NotNull;

public interface VehicleComponent {

    /**
     * Get the {@link VehicleComponentType type} of this component.
     *
     * @return The type of this component.
     */
    @NotNull
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
     * <p>
     * This is the phase where components accumulate forces
     * ({@link ca.bkaw.torque.components.RigidBodyComponent#addForce}) and register
     * velocity constraints
     * ({@link ca.bkaw.torque.components.RigidBodyComponent#addConstraint}). Nothing is
     * integrated yet, so the order components tick in does not affect the physics.
     *
     * @param vehicle The vehicle this component belongs to.
     */
    void tick(Vehicle vehicle);

    /**
     * Called each tick after all components have {@link #tick ticked}.
     * <p>
     * This is where {@link ca.bkaw.torque.components.RigidBodyComponent} solves the
     * registered constraints and integrates the accumulated forces, guaranteeing that
     * everything registered during the tick phase - regardless of component order -
     * takes effect in the same tick.
     *
     * @param vehicle The vehicle this component belongs to.
     */
    default void postTick(Vehicle vehicle) {}
}
