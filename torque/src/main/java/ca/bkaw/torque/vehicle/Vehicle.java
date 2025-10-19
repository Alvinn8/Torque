package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.Torque;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A loaded vehicle in the world. Components perform all logic.
 */
public class Vehicle {
    private final @NotNull Torque torque;
    private final @NotNull VehicleType type;
    private final List<VehicleComponent> components = new ArrayList<>();

    public Vehicle(@NotNull Torque torque, @NotNull VehicleType type) {
        this.torque = torque;
        this.type = type;
    }

    /**
     * Get a component on the vehicle.
     *
     * @param type The class of the component.
     * @return An optional containing the component if present, otherwise an empty optional.
     * @param <T> The type of component.
     */
    public <T extends VehicleComponent> Optional<T> getComponent(@NotNull Class<T> type) {
        for (VehicleComponent component : this.components) {
            if (type.isInstance(component)) {
                T obj = type.cast(component);
                return Optional.of(obj);
            }
        }
        return Optional.empty();
    }

    /**
     * Add a component to the vehicle.
     *
     * @param component The component to add.
     */
    public void addComponent(@NotNull VehicleComponent component) {
        this.components.add(component);
    }

    public List<VehicleComponent> getComponents() {
        return Collections.unmodifiableList(this.components);
    }

    public @NotNull Torque getTorque() {
        return this.torque;
    }

    public @NotNull VehicleType getType() {
        return this.type;
    }

    /**
     * Called each tick.
     */
    public void tick() {
        for (VehicleComponent component : this.components) {
            component.tick(this);
        }
    }
}
