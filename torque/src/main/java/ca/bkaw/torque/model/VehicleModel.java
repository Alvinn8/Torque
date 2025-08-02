package ca.bkaw.torque.model;

import ca.bkaw.torque.assets.model.Model;
import ca.bkaw.torque.platform.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VehicleModel {
    private final Identifier identifier;
    private @Nullable Model model;
    private final VehicleModelPart inline;
    private final VehicleModelPart hologram;
    private final VehicleModelPart primary;
    private final List<VehicleModelPart> parts;
    private Map<Class<? extends TagHandler<?>>, Object> tagHandlerData;

    public VehicleModel(Identifier identifier, @NotNull Model model, VehicleModelPart primary, List<VehicleModelPart> parts, Map<Class<? extends TagHandler<?>>, Object> tagHandlerData) {
        this.identifier = identifier;
        this.model = model;
        this.primary = primary;
        this.inline = null;
        this.hologram = null;
        this.parts = parts;
        this.tagHandlerData = tagHandlerData;
    }

    public Identifier getIdentifier() {
        return this.identifier;
    }

    public VehicleModelPart getPrimary() {
        return this.primary;
    }

    public List<VehicleModelPart> getParts() {
        return this.parts;
    }

    public @Nullable Model getModel() {
        return this.model;
    }

    public void unsetModel() {
        this.model = null;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTagData(@NotNull Class<? extends TagHandler<T>> tagHandlerClass) {
        return Optional.ofNullable((T) this.tagHandlerData.get(tagHandlerClass));
    }
}
