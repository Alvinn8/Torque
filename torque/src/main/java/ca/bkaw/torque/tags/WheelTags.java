package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import ca.bkaw.torque.model.TagString;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

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

        int wheelIndex = 0;
        for (ModelGroup group : wheelGroups) {
            IntList elementIndexes = group.getAllElements();
            String name = "wheel" + (wheelIndex++);
            TagString tags = group.getTags();
            boolean steerable = tags.hasTag("steerable");
            boolean driven = tags.hasTag("driven");
            boolean parkingBrake = tags.hasTag("parking_brake");
            Wheel wheel = new Wheel(name, 0.5, steerable, driven, parkingBrake);
            modelExtractor.addExtraction(name, wheel, elementIndexes);
            wheels.add(wheel);
        }

        return wheels;
    }
}
