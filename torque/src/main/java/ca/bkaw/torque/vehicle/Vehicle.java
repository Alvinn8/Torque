package ca.bkaw.torque.vehicle;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A loaded vehicle in the world. Components perform all logic.
 */
public class Vehicle {
    private final List<VehicleComponent> components = new ArrayList<>();

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
}
