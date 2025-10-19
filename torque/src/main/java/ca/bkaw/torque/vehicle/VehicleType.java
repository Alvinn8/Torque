package ca.bkaw.torque.vehicle;

import ca.bkaw.torque.model.VehicleModel;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.util.InertiaTensor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.joml.Matrix3d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record VehicleType(
    Identifier identifier,
    VehicleModel model,
    List<ComponentConfiguration> components,
    double mass, // unit: kilogram
    Matrix3d localInertiaTensorInverse // unit: (kg m^2)^-1, local to the unrotated vehicle's coordinate system
) {

    record ComponentConfiguration(VehicleComponentType type, Object configuration) {}

    /**
     * Read a VehicleType from a JSON object.
     *
     * @param vehicleManager The vehicle manager to use for looking up component types and vehicle models.
     * @param identifier The identifier of the vehicle type.
     * @param json The JSON object containing the vehicle type data.
     * @return A VehicleType instance.
     * @throws IOException If an I/O error occurs while reading the vehicle model.
     */
    public static VehicleType fromJson(VehicleManager vehicleManager, Identifier identifier, JsonObject json) throws IOException {
        String modelString = json.get("model").getAsString();
        Identifier modelIdentifier = Identifier.fromString(modelString);

        VehicleModel model = vehicleManager.getTorque().getAssets().getOrCreateVehicleModel(modelIdentifier);
        if (model == null || model.getModel() == null || model.getModel().getAllElements() == null) {
            throw new IllegalArgumentException("Vehicle model not found: " + modelIdentifier);
        }


        double mass = json.get("mass_kg").getAsDouble();
        if (mass <= 0) {
            throw new IllegalArgumentException("Vehicle mass must be greater than 0, got: " + mass);
        }

        Matrix3d localInertiaTensor = InertiaTensor.calculateInertiaTensor(mass, model.getModel().getAllElements());
        Debug.print("localInertiaTensor = \n" + localInertiaTensor);
        Matrix3d localInertiaTensorInverse = new Matrix3d(localInertiaTensor).invert();

        ArrayList<ComponentConfiguration> components = new ArrayList<>();

        for (JsonElement componentJsonElement : json.get("components").getAsJsonArray()) {
            JsonObject componentJson = componentJsonElement.getAsJsonObject();
            String componentTypeString = componentJson.get("type").getAsString();
            Identifier componentTypeIdentifier = Identifier.fromString(componentTypeString);
            VehicleComponentType vehicleComponentType = vehicleManager.getComponentTypeRegistry().get(componentTypeIdentifier);
            if (vehicleComponentType == null) {
                throw new IllegalArgumentException("Unknown vehicle component type: " + componentTypeIdentifier);
            }
            Object componentConfig = vehicleComponentType.configParser().apply(componentJson);
            components.add(new ComponentConfiguration(
                vehicleComponentType,
                componentConfig
            ));
        }

        components.trimToSize();

        return new VehicleType(identifier, model, Collections.unmodifiableList(components), mass, localInertiaTensorInverse);
    }
}
