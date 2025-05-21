package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.model.VehicleModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A loaded vehicle in the world. Components perform all logic.
 */
public class Vehicle {
    private final List<VehicleComponent> components = new ArrayList<>();
    private final @NotNull VehicleModel model;

    public Vehicle(@NotNull VehicleModel model) {
        this.model = model;
    }

    /**
     * Get a component on the vehicle.
     *
     * @param type The class of the component.
     * @return An optional containing the component if present, otherwise an empty optional.
     * @param <T> The type of component.
     */
    public <T extends VehicleComponent> Optional<T> getComponent(@NotNull Class<T> type) {
        return this.components.stream()
            .filter(type::isInstance)
            .map(type::cast)
            .findFirst();
    }

    /**
     * Add a component to the vehicle.
     *
     * @param component The component to add.
     */
    public void addComponent(@NotNull VehicleComponent component) {
        this.components.add(component);
    }

    public @NotNull VehicleModel getModel() {
        return this.model;
    }

    /**
     * Called each tick.
     */
    public void tick() {
        this.components.forEach(VehicleComponent::tick);
    }
}
