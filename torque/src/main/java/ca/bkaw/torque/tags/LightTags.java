package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * A tag handler that extracts groups tagged with {@code #light=<name>} into
 * separate model parts so components can toggle their glow individually.
 * <p>
 * Extracted parts carry a {@link Light} as their part data, which is how
 * components should identify light parts in
 * {@code PartTransformationProvider#getPartTransform} rather than by matching
 * on the part name.
 */
public class LightTags implements TagHandler<Set<String>> {
    /**
     * Part data attached to extracted light parts.
     *
     * @param name The light name, the value of the {@code #light=<name>} tag.
     */
    public record Light(@NotNull String name) {}

    @Override
    public Set<String> process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        Set<String> lightNames = new HashSet<>();
        for (ModelGroup modelGroup : model.getGroupsByTag("light")) {
            String lightName = modelGroup.getTags().getTagValue("light");
            if (lightName == null) {
                continue;
            }
            // Extract all elements in the group as a separate model.
            modelExtractor.addExtraction("light_" + lightName, new Light(lightName), modelGroup.getAllElements());
            lightNames.add(lightName);
        }
        return lightNames.isEmpty() ? null : lightNames;
    }
}
