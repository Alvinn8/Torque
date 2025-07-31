package ca.bkaw.torque.assets.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A list of {@link ModelElement}s.
 */
public class ModelElementList {
    private final @NotNull JsonArray json;
    private final List<@Nullable ModelElement> elements;

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
        Vector3d min = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (ModelElement element : this.getElements()) {
            Vector3d elementMiddle = element.getMiddle();
            if (elementMiddle.x < min.x) min.x = elementMiddle.x;
            if (elementMiddle.y < min.y) min.y = elementMiddle.y;
            if (elementMiddle.z < min.z) min.z = elementMiddle.z;
            if (elementMiddle.x > max.x) max.x = elementMiddle.x;
            if (elementMiddle.y > max.y) max.y = elementMiddle.y;
            if (elementMiddle.z > max.z) max.z = elementMiddle.z;
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
    public Vector3d centerGeometrically() {
        Vector3d middle = this.getMiddle();
        Vector3d diff = new Vector3d(8, 8, 8).sub(middle);
        this.move(diff);
        return diff;
    }

    /**
     * Get the size of the model, measured in blocks (one block = 16x16x16 units in the
     * model). The game allows a maximum block size of 3. If a model has a block size
     * above 3, it will not render in game.
     * <p>
     * The block size is not an accurate measurement of the visual model size. This is because
     * rotated elements that appear to be outside the block size may still count as inside.
     * <p>
     * When the model is scaled, the block size is also scaled accordingly.
     *
     * @return The block size of the model.
     */
    public double getBlockSize() {
        double size = 0;

        for (ModelElement element : this.getElements()) {
            Vector3d from = element.getFrom();
            Vector3d to = element.getTo();
            for (Vector3d vector : List.of(from, to)) {
                Vector3d fromMiddle = vector.sub(8, 8, 8).absolute();
                if (fromMiddle.x > size) size = fromMiddle.x;
                if (fromMiddle.y > size) size = fromMiddle.y;
                if (fromMiddle.z > size) size = fromMiddle.z;
            }
        }

        // The size is in units ("pixels"), but we want it in blocks.
        // It measures from the center to the edge, so we multiply by 2 to get the full size.
        return 2.0 * size / 16.0;
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
     * Remove elements that match the predicate.
     *
     * @param predicate The predicate.
     */
    public void removeIf(Predicate<ModelElement> predicate) {
        for (int i = this.elements.size() - 1; i >= 0; i--) {
            ModelElement element = this.getElement(i);
            if (predicate.test(element)) {
                this.elements.remove(i);
                this.json.remove(i);
            }
        }
    }

    /**
     * Remove the element at the specified index.
     *
     * @param index The index of the element to remove.
     */
    public void remove(int index) {
        this.elements.remove(index);
        this.json.remove(index);
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