package ca.bkaw.torque.assets.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A wrapper around a model in a resource pack.
 * <p>
 * Changes made to the model and its element will be reflected on the wrapped JSON.
 */
public class Model {
    public static final String ELEMENTS = "elements";
    public static final String TEXTURES = "textures";
    public static final String GROUPS = "groups";

    private final @NotNull JsonObject json;
    private @Nullable ModelElementList elements;

    /**
     * Create a model that wraps around the JSON object as the root of the model.
     *
     * @param json The JSON root of the model.
     */
    public Model(@NotNull JsonObject json) {
        this.json = json;
    }

    /**
     * Create an empty model.
     */
    public Model() {
        this(new JsonObject());
    }

    /**
     * Convert a JSON array with length 3 to a vector.
     *
     * @param array The array of length 3, holding only numbers.
     * @return The vector.
     */
    @NotNull
    public static Vector3d jsonArrayToVector(@NotNull JsonArray array) {
        return new Vector3d(
            array.get(0).getAsDouble(),
            array.get(1).getAsDouble(),
            array.get(2).getAsDouble()
        );
    }

    /**
     * Update the JSON array to describe the vector.
     *
     * @param vector The vector.
     * @param array The JSON array to update.
     */
    static void vectorToJsonArray(@NotNull Vector3d vector, @NotNull JsonArray array) {
        array.set(0, new JsonPrimitive(vector.x));
        array.set(1, new JsonPrimitive(vector.y));
        array.set(2, new JsonPrimitive(vector.z));
    }

    /**
     * Get all the elements in this model. Returns null if the model has no elements.
     * <p>
     * Changing the list or the elements in the list will modify the model json.
     *
     * @return The list of all elements.
     */
    @Nullable
    public ModelElementList getAllElements() {
        if (this.elements == null) {
            if (!this.json.has(ELEMENTS)) {
                return null;
            }
            JsonArray json = this.json.getAsJsonArray(ELEMENTS);
            this.elements = new ModelElementList(json);
        }
        return this.elements;
    }

    /**
     * Set the elements of this model. Setting to null removes the elements from the
     * model JSON.
     *
     * @param list The list of elements, or null.
     */
    public void setElements(@Nullable ModelElementList list) {
        this.elements = list;
        if (this.elements == null) {
            this.json.remove(ELEMENTS);
        } else {
            this.json.add(ELEMENTS, this.elements.getJson());
        }
    }

    /**
     * Return a {@link ModelElementList} from a list of indexes to the elements.
     *
     * @param indexes The list of indexes.
     * @return The list.
     * @throws IllegalStateException if the model has no elements.
     */
    @NotNull
    public ModelElementList getElements(IntList indexes) {
        ModelElementList allElements = this.getAllElements();
        if (allElements == null) {
            throw new IllegalStateException("This model has no elements.");
        }
        ModelElementList list = new ModelElementList(new JsonArray(indexes.size()));
        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.getInt(i);
            ModelElement element = allElements.getElement(index);
            list.add(element);
        }
        return list;
    }

    /**
     * Get the list of Blockbench groups in this model. Returns null if the model has
     * no groups.
     * <p>
     * Mutating the list does not change the model json.
     *
     * @return The list of groups.
     */
    @Nullable
    public List<ModelGroup> getGroups() {
        if (!this.json.has(GROUPS)) {
            return null;
        }
        JsonArray groups = this.json.getAsJsonArray(GROUPS);
        List<ModelGroup> list = new ArrayList<>();
        for (JsonElement group : groups) {
            if (group.isJsonObject()) {
                list.add(new ModelGroup(group.getAsJsonObject()));
            }
        }
        return list;
    }

    /**
     * Remove all groups from this model.
     */
    public void removeGroups() {
        this.json.remove(GROUPS);
    }

    /**
     * Get the JSON object this model is wrapping.
     *
     * @return The json.
     */
    @NotNull
    public JsonObject getJson() {
        return this.json;
    }

    /**
     * Create a deep copy of this model.
     *
     * @return The copy.
     */
    @NotNull
    public Model deepCopy() {
        return new Model(this.json.deepCopy());
    }

    /**
     * Get all elements that have the specified tag.
     * 
     * @param tag The tag to search for
     * @return A list of elements with the specified tag
     */
    @NotNull
    public List<ModelElement> getElementsByTag(@NotNull String tag) {
        List<ModelElement> result = new ArrayList<>();
        ModelElementList allElements = this.getAllElements();
        if (allElements != null) {
            for (ModelElement element : allElements.getElements()) {
                if (element.getTags().hasTag(tag)) {
                    result.add(element);
                }
            }
        }
        return result;
    }

    /**
     * Get all groups that have the specified tag.
     * 
     * @param tag The tag to search for
     * @return A list of groups with the specified tag
     */
    @NotNull
    public List<ModelGroup> getGroupsByTag(@NotNull String tag) {
        List<ModelGroup> result = new ArrayList<>();
        List<ModelGroup> allGroups = this.getGroups();
        if (allGroups != null) {
            for (ModelGroup group : allGroups) {
                if (group.getTags().hasTag(tag)) {
                    result.add(group);
                }
            }
        }
        return result;
    }

    /**
     * Get the texture mapping of this model.
     *
     * @return A map of texture names to their identifiers.
     */
    @Nullable
    public Map<String, String> getTextures() {
        JsonObject json = this.json.getAsJsonObject(TEXTURES);
        if (json == null) {
            return null;
        }
        HashMap<String, String> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            map.put(key, value.getAsString());
        }
        return map;
    }

    /**
     * Set the texture mapping of this model.
     *
     * @param textures The map of textures.
     */
    public void setTextures(@Nullable Map<String, String> textures) {
        if (textures == null) {
            this.json.remove(TEXTURES);
            return;
        }
        JsonObject json = new JsonObject();
        for (Map.Entry<String, String> entry : textures.entrySet()) {
            json.addProperty(entry.getKey(), entry.getValue());
        }
        this.json.add(TEXTURES, json);
    }
}