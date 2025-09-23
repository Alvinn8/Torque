package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.DataInput;
import com.google.gson.JsonObject;

import java.util.function.BiFunction;
import java.util.function.Function;

public record VehicleComponentType(
    Identifier identifier,
    Function<JsonObject, ?> configParser,
    VehicleComponentConstructor<?> constructor
) {
    @FunctionalInterface
    public interface VehicleComponentConstructor<T> {
        VehicleComponent create(Vehicle vehicle, T config, DataInput data);

        /**
         * Call the constructor and assume the configuration is of the correct type.
         *
         * @param vehicle the vehicle
         * @param config the configuration, assumed to be of type T
         * @param data an data input
         * @return the created vehicle component
         */
        default VehicleComponent createUnsafe(Vehicle vehicle, Object config, DataInput data) {
            @SuppressWarnings("unchecked")
            T tConfig = (T) config;
            return this.create(vehicle, tConfig, data);
        }
    }

    public static VehicleComponentType create(Identifier identifier, BiFunction<Vehicle, DataInput, VehicleComponent> constructor) {
        return new VehicleComponentType(
            identifier,
            Function.identity(),
            (vehicle, config, data) -> constructor.apply(vehicle, data)
        );
    }

    public static Builder<JsonObject> builder(Identifier identifier) {
        return new Builder<>(identifier, Function.identity());
    }

    public static class Builder<T> {
        private final Identifier identifier;
        private final Function<JsonObject, T> configParser;

        public Builder(Identifier identifier, Function<JsonObject, T> configParser) {
            this.identifier = identifier;
            this.configParser = configParser;
        }

        public <S> Builder<S> configParser(Function<JsonObject, S> configParser) {
            return new Builder<>(this.identifier, configParser);
        }

        public VehicleComponentType create(VehicleComponentConstructor<T> constructor) {
            return new VehicleComponentType(this.identifier, this.configParser, constructor);
        }
    }
}
