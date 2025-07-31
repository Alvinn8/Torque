package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A tag handler that processes groups tagged with #steering_wheel.
 * <p>
 * This is a group-based handler, meaning all elements within a tagged group
 * are extracted together into a separate model.
 */
public class SteeringWheelTagHandler implements TagHandler<Void> {

    @Override
    public Void process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        List<ModelGroup> steeringWheelGroups = model.getGroupsByTag("steering_wheel");
        
        if (steeringWheelGroups.isEmpty()) {
            return null;
        }

        for (ModelGroup group : steeringWheelGroups) {
            IntList elementIndexes = group.getAllElements();
            modelExtractor.addExtraction("steering_wheel", elementIndexes);
        }

        return null;
    }
}
