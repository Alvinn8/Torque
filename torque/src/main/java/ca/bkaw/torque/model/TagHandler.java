package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.ModelElement;
import ca.bkaw.torque.assets.model.ModelGroup;
import org.jetbrains.annotations.NotNull;

/**
 * A handler for tagged elements in a model.
 */
public interface TagHandler {
    boolean handleTaggedElement(@NotNull ModelElement element);

    boolean handleTaggedGroup(@NotNull ModelGroup group);
}
