package ca.bkaw.torque.components;

import ca.bkaw.torque.light.LightPattern;
import ca.bkaw.torque.light.LightState;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.tags.LightTags;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * A component that flashes the vehicle's lights in configurable
 * {@link LightPattern}s: police emergency lights, amber warning lights on
 * trucks blocking a road, stop signals, and similar.
 * <p>
 * The component is configured with named <i>modes</i>, each mode being one
 * {@link LightPattern}. One mode at a time can be active, selected with
 * {@link #setActiveMode(String)}. Example configuration:
 * <pre>{@code
 * {
 *     "type": "torque:warning_lights",
 *     "active": "emergency",
 *     "modes": {
 *         "emergency": { "tracks": [ ... ] },
 *         "road_block": { "tracks": [ ... ] }
 *     }
 * }
 * }</pre>
 * The lights referenced by the patterns are the model parts extracted by
 * {@link LightTags}, addressed without the {@code light_} prefix.
 */
public class WarningLightsComponent implements VehicleComponent, PartTransformationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.builder(
            new Identifier("torque", "warning_lights")
        )
        .configParser(WarningLightsComponent::parseConfig)
        .create(WarningLightsComponent::new);

    /**
     * The configuration of a {@link WarningLightsComponent}.
     *
     * @param modes The named light patterns this vehicle can run.
     * @param defaultMode The mode that is active when the vehicle is created,
     *                    or null to start with all warning lights off.
     */
    public record Config(
        @Unmodifiable @NotNull Map<String, LightPattern> modes,
        @Nullable String defaultMode
    ) {}

    private static Config parseConfig(JsonObject json) {
        JsonObject modesJson = json.getAsJsonObject("modes");
        if (modesJson == null) {
            throw new IllegalArgumentException("warning_lights component is missing \"modes\".");
        }
        Map<String, LightPattern> modes = new HashMap<>();
        for (String modeName : modesJson.keySet()) {
            modes.put(modeName, LightPattern.fromJson(modesJson.getAsJsonObject(modeName)));
        }
        String defaultMode = json.has("active") ? json.get("active").getAsString() : null;
        if (defaultMode != null && !modes.containsKey(defaultMode)) {
            throw new IllegalArgumentException("Unknown active warning lights mode: " + defaultMode);
        }
        return new Config(Map.copyOf(modes), defaultMode);
    }

    private final @NotNull Config config;
    private @Nullable String activeMode;
    private long timeTicks = 0;

    public WarningLightsComponent(Vehicle vehicle, Config config, DataInput data) {
        this.config = config;
        this.activeMode = config.defaultMode();
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {
        // TODO persist the active mode once DataOutput supports strings.
    }

    @Override
    public void tick(Vehicle vehicle) {
        this.timeTicks++;
    }

    /**
     * Get the modes this component is configured with.
     *
     * @return The map of mode name to pattern.
     */
    @Unmodifiable
    @NotNull
    public Map<String, LightPattern> getModes() {
        return this.config.modes();
    }

    /**
     * Get the name of the currently active mode.
     *
     * @return The mode name, or null if all warning lights are off.
     */
    @Nullable
    public String getActiveMode() {
        return this.activeMode;
    }

    /**
     * Set the active mode, restarting its pattern from the beginning. Pass
     * null to turn all warning lights off.
     *
     * @param modeName The name of a configured mode, or null.
     * @throws IllegalArgumentException If the mode does not exist.
     */
    public void setActiveMode(@Nullable String modeName) {
        if (modeName != null && !this.config.modes().containsKey(modeName)) {
            throw new IllegalArgumentException("Unknown warning lights mode: " + modeName);
        }
        this.activeMode = modeName;
        this.timeTicks = 0;
    }

    @Override
    public @Nullable PartTransform getPartTransform(@NotNull String partName, @Nullable Object partData, @NotNull Vehicle vehicle) {
        if (!partName.startsWith(LightTags.PART_NAME_PREFIX)) {
            return null;
        }
        String lightName = partName.substring(LightTags.PART_NAME_PREFIX.length());

        LightPattern pattern = this.activeMode != null
            ? this.config.modes().get(this.activeMode)
            : null;
        if (pattern != null) {
            LightState state = pattern.getState(lightName, this.timeTicks);
            if (state != null) {
                return new PartTransform(
                    new Quaternionf(), new Vector3f(),
                    state.on(), state.on() ? state.color() : null
                );
            }
        }

        // The light is not in the active pattern. If some mode uses this
        // light, this component owns it and reports it as off. Lights no mode
        // uses are left for other components (like turn signals).
        for (LightPattern mode : this.config.modes().values()) {
            if (mode.controls(lightName)) {
                return new PartTransform(new Quaternionf(), new Vector3f(), false, null);
            }
        }
        return null;
    }
}
