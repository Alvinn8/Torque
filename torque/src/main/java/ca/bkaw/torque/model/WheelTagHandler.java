package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A tag handler that processes groups tagged with #wheel.
 * This is a group-based handler that can handle multiple wheel groups
 * (e.g., 4 wheels in a car) and extracts them into separate models.
 */
public class WheelTagHandler implements TagHandler<Void> {

    @Override
    public Void process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        List<ModelGroup> wheelGroups = model.getGroupsByTag("wheel");

        if (wheelGroups.isEmpty()) {
            return null;
        }

        int wheelIndex = 0;
        for (ModelGroup group : wheelGroups) {
            IntList elementIndexes = group.getAllElements();
            modelExtractor.addExtraction("wheel" + (wheelIndex++), elementIndexes);
        }

        return null;
    }
}
