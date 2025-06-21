package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.platform.Identifier;

import java.util.function.Function;

public record VehicleComponentType(
    Identifier identifier,
    Function<Vehicle, VehicleComponent> constructor
) {
    public static VehicleComponentType create(Identifier identifier, Function<Vehicle, VehicleComponent> constructor) {
        return new VehicleComponentType(identifier, constructor);
    }
}
