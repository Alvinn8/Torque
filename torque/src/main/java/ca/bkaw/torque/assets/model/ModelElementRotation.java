package ca.bkaw.torque.assets.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

/**
 * Rotation for a {@link ModelElement}.
 */
public class ModelElementRotation {
    public static final String ORIGIN = "origin";

    private final JsonObject json;

    public ModelElementRotation(@NotNull JsonObject json) {
        this.json = json;
    }

    /**
     * Get the origin for the rotation.
     * <p>
     * Mutating the vector does not affect the element. New instances are returned for
     * each call to the method.
     *
     * @return The rotation origin.
     */
    @NotNull
    public Vector3d getOrigin() {
        JsonArray origin = this.json.getAsJsonArray(ORIGIN);
        return Model.jsonArrayToVector(origin);
    }

    /**
     * Set the origin for the rotation.
     *
     * @param vector The origin vector.
     */
    public void setOrigin(@NotNull Vector3d vector) {
        JsonArray array = this.json.getAsJsonArray(ORIGIN);
        Model.vectorToJsonArray(vector, array);
    }

    /**
     * Get the JSON object that this rotation is wrapping.
     *
     * @return The json.
     */
    @NotNull
    public JsonObject getJson() {
        return json;
    }

    /**
     * Create a deep copy of this rotation.
     *
     * @return The copy.
     */
    @NotNull
    public ModelElementRotation deepCopy() {
        return new ModelElementRotation(this.json.deepCopy());
    }
}