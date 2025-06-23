package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record VehicleType(
    Identifier identifier,
    VehicleModel model,
    List<ComponentConfiguration> components,
    double mass // unit: kilogram
) {

    record ComponentConfiguration(VehicleComponentType type, Object configuration) {}

    /**
     * Read a VehicleType from a JSON object.
     *
     * @param vehicleManager The vehicle manager to use for looking up component types and vehicle models.
     * @param identifier The identifier of the vehicle type.
     * @param json The JSON object containing the vehicle type data.
     * @return A VehicleType instance.
     */
    public static VehicleType fromJson(VehicleManager vehicleManager, Identifier identifier, JsonObject json) {
        String modelString = json.get("model").getAsString();
        Identifier modelIdentifier = Identifier.fromString(modelString);

        VehicleModel model = vehicleManager.getTorque().getAssets().getVehicleModelRegistry().get(modelIdentifier);

        double mass = json.get("mass_kg").getAsDouble();
        if (mass <= 0) {
            throw new IllegalArgumentException("Vehicle mass must be greater than 0, got: " + mass);
        }

        ArrayList<ComponentConfiguration> components = new ArrayList<>();

        for (JsonElement componentJsonElement : json.get("components").getAsJsonArray()) {
            JsonObject componentJson = componentJsonElement.getAsJsonObject();
            String componentTypeString = componentJson.get("type").getAsString();
            Identifier componentTypeIdentifier = Identifier.fromString(componentTypeString);
            VehicleComponentType vehicleComponentType = vehicleManager.getComponentTypeRegistry().get(componentTypeIdentifier);
            if (vehicleComponentType == null) {
                throw new IllegalArgumentException("Unknown vehicle component type: " + componentTypeIdentifier);
            }
            components.add(new ComponentConfiguration(
                vehicleComponentType,
                componentJson
            ));
        }

        components.trimToSize();

        return new VehicleType(identifier, model, Collections.unmodifiableList(components), mass);
    }

}
