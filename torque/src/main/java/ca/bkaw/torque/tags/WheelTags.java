package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import ca.bkaw.torque.model.TagString;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

/**
 * A tag handler that processes groups tagged with #wheel.
 * <p>
 * This is a group-based handler that can handle multiple wheel groups
 * (e.g., 4 wheels in a car) and extracts them into separate models.
 */
public class WheelTags implements TagHandler<Void> {

    public record Wheel(
        String partName,
        double radius,
        boolean steerable,
        boolean driven,
        boolean parkingBrake
    ) {}

    @Override
    public Void process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        List<ModelGroup> wheelGroups = model.getGroupsByTag("wheel");

        if (wheelGroups.isEmpty()) {
            return null;
        }

        int wheelIndex = 0;
        for (ModelGroup group : wheelGroups) {
            IntList elementIndexes = group.getAllElements();
            String name = "wheel" + (wheelIndex++);
            modelExtractor.addExtraction(name, elementIndexes);
            TagString tags = group.getTags();
            boolean steerable = tags.hasTag("steerable");
            boolean driven = tags.hasTag("driven");
            boolean parkingBrake = tags.hasTag("parking_brake");
        }

        return null;
    }
}
