package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.TorqueAssets;
import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelElement;
import ca.bkaw.torque.assets.model.ModelElementList;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import ca.bkaw.torque.model.TagString;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A tag handler that processes groups tagged with #wheel.
 * <p>
 * This is a group-based handler that can handle multiple wheel groups
 * (e.g., 4 wheels in a car) and extracts them into separate models.
 */
public class WheelTags implements TagHandler<List<WheelTags.Wheel>> {

    public record Wheel(
        String partName,
        Vector3d contactPatch,
        double radius,
        boolean steerable,
        boolean driven,
        boolean parkingBrake
    ) {}

    @Override
    public List<Wheel> process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        List<ModelGroup> wheelGroups = model.getGroupsByTag("wheel");
        List<Wheel> wheels = new ArrayList<>();

        if (wheelGroups.isEmpty()) {
            return null;
        }

        ModelElementList modelElements = model.getAllElements();
        if (modelElements == null) {
            return null;
        }

        int wheelIndex = 0;
        for (ModelGroup group : wheelGroups) {
            IntList elementIndexes = group.getAllElements();
            Vector3d min = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
            Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
            for (int index : elementIndexes) {
                ModelElement element = modelElements.getElement(index);
                min.min(element.getFrom());
                max.max(element.getTo());
            }
            // Average x, z and min y to get the contact patch center.
            Vector3d contactPatch = TorqueAssets.getElementOffset(new Vector3d(
                (min.x + max.x) / 2,
                min.y,
                (min.z + max.z) / 2
            )); // unit: meters
            String name = "wheel" + (wheelIndex++);
            TagString tags = group.getTags();
            boolean steerable = tags.hasTag("steerable");
            boolean driven = tags.hasTag("driven");
            boolean parkingBrake = tags.hasTag("parking_brake");
            Wheel wheel = new Wheel(name, contactPatch, 0.5, steerable, driven, parkingBrake);
            modelExtractor.addExtraction(name, wheel, elementIndexes);
            wheels.add(wheel);
        }

        return wheels;
    }
}
