package ca.bkaw.torque.assets.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A list of {@link ModelElement}s.
 */
public class ModelElementList {
    private final JsonArray json;
    private final List<ModelElement> elements;

    public ModelElementList(@NotNull JsonArray json) {
        this.json = json;
        this.elements = new ArrayList<>(json.size());
        for (int i = 0; i < json.size(); i++) {
            this.elements.add(null);
        }
    }

    /**
     * Move the elements by the specified vector.
     *
     * @param vector The vector with amount to move the elements by.
     */
    public void move(@NotNull Vector3d vector) {
        for (ModelElement element : this.getElements()) {
            element.move(vector);
        }
    }

    /**
     * Scale all the elements in the list around the specified origin.
     *
     * @param scale The factor to scale by.
     * @param origin The origin to scale around.
     */
    public void scale(@NotNull Vector3d scale, @NotNull Vector3d origin) {
        for (ModelElement element : this.getElements()) {
            element.scale(scale, origin);
        }
    }

    /**
     * Get the geometric center of the elements.
     *
     * @return The middle point.
     */
    @NotNull
    public Vector3d getMiddle() {
        Vector3d min = new Vector3d(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
        Vector3d max = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
        for (ModelElement element : this.elements) {
            Vector3d elementMiddle = element.getMiddle();
            if (elementMiddle.x > min.x) min.x = elementMiddle.x;
            if (elementMiddle.y > min.y) min.y = elementMiddle.y;
            if (elementMiddle.z > min.z) min.z = elementMiddle.z;
            if (elementMiddle.x < max.x) max.x = elementMiddle.x;
            if (elementMiddle.y < max.y) max.y = elementMiddle.y;
            if (elementMiddle.z < max.z) max.z = elementMiddle.z;
        }
        return min.lerp(max, 0.5);
    }

    /**
     * Center this list of elements at the origin (8, 8, 8) and return the vector that
     * the elements were moved by.
     * <p>
     * Not to be confused with {@link #getMiddle()}.
     *
     * @return The vector elements were moved by.
     */
    @NotNull
    public Vector3d center() {
        Vector3d middle = this.getMiddle();
        Vector3d diff = new Vector3d(8, 8, 8).sub(middle);
        this.move(diff);
        return diff;
    }

    /**
     * Get the {@link ModelElement} at the index.
     *
     * @param index The index.
     * @return The model element.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @NotNull
    public ModelElement getElement(int index) {
        ModelElement element = this.elements.get(index);
        if (element == null) {
            JsonObject json = this.json.get(index).getAsJsonObject();
            element = new ModelElement(json);
            this.elements.set(index, element);
        }
        return element;
    }

    /**
     * Return an unmodifiable view of all the wrapped elements.
     *
     * @return The list.
     */
    @NotNull
    @UnmodifiableView
    public List<ModelElement> getElements() {
        for (int i = 0; i < this.elements.size(); i++) {
            if (this.elements.get(i) == null) {
                // Ensure the element has been wrapped
                this.getElement(i);
            }
        }
        return Collections.unmodifiableList(this.elements);
    }

    /**
     * Add an element to the list.
     *
     * @param element The wrapper around the element to add.
     */
    public void add(@NotNull ModelElement element) {
        this.json.add(element.getJson());
        this.elements.add(element);
    }

    /**
     * Get the JSON array of elements this list is wrapping.
     *
     * @return The json.
     */
    @NotNull
    public JsonArray getJson() {
        return this.json;
    }

    /**
     * Create a deep copy of this list and its elements.
     *
     * @return The copy.
     */
    @NotNull
    public ModelElementList deepCopy() {
        return new ModelElementList(this.json.deepCopy());
    }
}