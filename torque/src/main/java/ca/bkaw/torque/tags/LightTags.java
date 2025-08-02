package ca.bkaw.torque.tags;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.assets.model.ModelExtractor;
import ca.bkaw.torque.assets.model.ModelGroup;
import ca.bkaw.torque.model.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class LightTags implements TagHandler<Set<String>> {
    @Override
    public Set<String> process(@NotNull Model model, @NotNull ModelExtractor modelExtractor) {
        Set<String> lightPartNames = new HashSet<>();
        for (ModelGroup modelGroup : model.getGroupsByTag("light")) {
            String lightName = modelGroup.getTags().getTagValue("light");
            if (lightName == null) {
                continue;
            }
            // Extract all elements in the group as a separate model.
            modelExtractor.addExtraction("light_" + lightName, modelGroup.getAllElements());
        }
        return lightPartNames;
    }
}
