package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.DataInput;

import java.util.function.BiFunction;

public record VehicleComponentType(
    Identifier identifier,
    BiFunction<Vehicle, DataInput, VehicleComponent> constructor
) {
    public static VehicleComponentType create(Identifier identifier, BiFunction<Vehicle, DataInput, VehicleComponent> constructor) {
        return new VehicleComponentType(identifier, constructor);
    }
}
