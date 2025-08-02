package ca.bkaw.torque.assets.model;

import ca.bkaw.torque.model.TagString;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.Set;

/**
 * A wrapper around an element in a {@link Model}.
 */
public class ModelElement {
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String ROTATION = "rotation";

    private final JsonObject json;
    private @Nullable ModelElementRotation rotation;
    private @Nullable TagString tagString;

    /**
     * Create a new wrapper around the element json.
     *
     * @param json The json for an element.
     */
    public ModelElement(@NotNull JsonObject json) {
        this.json = json;
    }

    /**
     * Create a new element that starts at {@code from} and ends at {@code to}.
     *
     * @param from The starting position.
     * @param to The end position.
     */
    public ModelElement(@NotNull Vector3d from, @NotNull Vector3d to) {
        this(new JsonObject());
        this.setFrom(from);
        this.setTo(to);
    }

    /**
     * Get the name of the element that is defined in the model.
     *
     * @return The name, or null.
     */
    public @Nullable String getName() {
        JsonElement name = this.json.get("name");
        if (name == null) {
            return null;
        }
        return name.getAsString();
    }

    public @NotNull TagString getTags() {
        if (this.tagString != null) {
            return this.tagString;
        }
        String name = this.getName();
        if (name == null) {
            return TagString.empty();
        }
        this.tagString = TagString.parse(name);
        return this.tagString;
    }

    /**
     * Move this element by the specified vector.
     *
     * @param vector The vector with amount to move the element by.
     */
    public void move(@NotNull Vector3d vector) {
        this.setFrom(this.getFrom().add(vector));
        this.setTo(this.getTo().add(vector));
        ModelElementRotation rotation = this.getRotation();
        if (rotation != null) {
            rotation.setOrigin(rotation.getOrigin().add(vector));
        }
    }

    /**
     * Get the middle point of the vector.
     *
     * @return The middle.
     */
    @NotNull
    public Vector3d getMiddle() {
        return this.getFrom().lerp(this.getTo(), 0.5);
    }

    /**
     * Scale the element around the specified origin.
     *
     * @param scale The factor to scale by.
     * @param origin The origin to scale around.
     */
    public void scale(Vector3d scale, Vector3d origin) {
        Vector3d from = this.getFrom();
        from.sub(origin).mul(scale).add(origin);
        this.setFrom(from);

        Vector3d to = this.getTo();
        to.sub(origin).mul(scale).add(origin);
        this.setTo(to);

        ModelElementRotation rotation = this.getRotation();
        if (rotation != null) {
            Vector3d rotationOrigin = rotation.getOrigin();
            rotationOrigin.sub(origin).mul(scale).add(origin);
            rotation.setOrigin(rotationOrigin);
        }
    }


    /**
     * Get the starting position of the element.
     * <p>
     * Mutating the vector does not affect the element. New instances are returned for
     * each call to the method.
     *
     * @return The starting position.
     */
    @NotNull
    public Vector3d getFrom() {
        JsonArray from = this.json.getAsJsonArray(FROM);
        return Model.jsonArrayToVector(from);
    }

    /**
     * Set the starting position of the element.
     *
     * @param vector The starting position vector.
     */
    public void setFrom(@NotNull Vector3d vector) {
        JsonArray array = this.json.getAsJsonArray(FROM);
        Model.vectorToJsonArray(vector, array);
    }

    /**
     * Get the end position of the element.
     * <p>
     * Mutating the vector does not affect the element. New instances are returned for
     * each call to the method.
     *
     * @return The end position.
     */
    @NotNull
    public Vector3d getTo() {
        JsonArray to = this.json.getAsJsonArray(TO);
        return Model.jsonArrayToVector(to);
    }

    /**
     * Set the end position of the element.
     *
     * @param vector The end position vector.
     */
    public void setTo(@NotNull Vector3d vector) {
        JsonArray array = this.json.getAsJsonArray(TO);
        Model.vectorToJsonArray(vector, array);
    }

    /**
     * Get the rotation of the element.
     *
     * @return The rotation, or null.
     */
    @Nullable
    public ModelElementRotation getRotation() {
        if (this.rotation == null) {
            if (!this.json.has(ROTATION)) {
                return null;
            }
            JsonObject json = this.json.getAsJsonObject(ROTATION);
            this.rotation = new ModelElementRotation(json);
        }
        return this.rotation;
    }

    /**
     * Set the rotation of the element. If set to null, the rotation will be removed.
     *
     * @param rotation The rotation.
     */
    public void setRotation(@Nullable ModelElementRotation rotation) {
        this.rotation = rotation;
        if (this.rotation == null) {
            this.json.remove(ROTATION);
        } else {
            this.json.add(ROTATION, rotation.getJson());
        }
    }

    /**
     * Get the JSON object this element is wrapping.
     *
     * @return The json.
     */
    @NotNull
    public JsonObject getJson() {
        return this.json;
    }

    /**
     * Create a deep copy of the element.
     *
     * @return The copy.
     */
    @NotNull
    public ModelElement deepCopy() {
        return new ModelElement(this.json.deepCopy());
    }
}