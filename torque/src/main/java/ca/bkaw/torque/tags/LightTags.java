package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Extracts groups tagged {@code #light=<name>} into separate model parts named
 * {@code light_<name>}, so that components can toggle and color each light
 * individually. The tag data is the set of light names found in the model
 * (without the {@code light_} prefix).
 */
public class LightTags implements TagHandler<Set<String>> {
    /**
     * The prefix that light model parts are named with. The part name is this
     * prefix followed by the light name from the {@code #light=<name>} tag.
     */
    public static final String PART_NAME_PREFIX = "light_";

    @Override
    public Set<String> process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        Set<String> lightNames = new HashSet<>();
        for (ModelGroup modelGroup : model.getGroupsByTag("light")) {
            String lightName = modelGroup.getTags().getTagValue("light");
            if (lightName == null) {
                continue;
            }
            // Extract all elements in the group as a separate model.
            modelExtractor.addExtraction(PART_NAME_PREFIX + lightName, null, modelGroup.getAllElements());
            lightNames.add(lightName);
        }
        return lightNames;
    }
}
