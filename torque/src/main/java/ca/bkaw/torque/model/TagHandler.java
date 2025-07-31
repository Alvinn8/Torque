package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import org.jetbrains.annotations.NotNull;

/**
 * A handler for processing tagged elements and groups in a model.
 * <p>
 * Tag handlers can process elements individually or groups of elements together.
 * 
 * @param <T> The type of data this handler produces
 */
public interface TagHandler<T> {
    /**
     * Process all tagged elements and groups in the model.
     * This method handles the coordination between element-based and group-based processing.
     * 
     * @param model The model to process
     * @return The processed result
     */
    T process(@NotNull Model model, @NotNull ModelExtractor modelExtractor);
}
