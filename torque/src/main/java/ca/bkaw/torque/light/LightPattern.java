package ca.bkaw.torque.light;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A flash pattern for a set of named lights on a vehicle.
 * <p>
 * A pattern is composed of {@link Track}s. Each track targets one or more
 * lights and flashes them with a {@link FlashSequence} in a color, optionally
 * shifted in time by a phase offset. All tracks run simultaneously.
 * <p>
 * Simple patterns use a few tracks: for example a classic alternating
 * ("wig-wag") pattern is two tracks with the same sequence where one track is
 * phase-shifted by half the period. The chaotic look of real emergency
 * lighting comes from layering tracks whose periods are not multiples of each
 * other (for example 14, 20 and 34 ticks): the tracks drift in and out of sync
 * instead of pulsing mechanically. See {@link LightPatterns} for ready-made
 * builders.
 * <p>
 * A pattern is stateless; the current {@link LightState} of a light is a pure
 * function of the time, queried with {@link #getState(String, long)}.
 *
 * @param tracks The tracks of the pattern.
 */
public record LightPattern(@NotNull List<Track> tracks) {
    public LightPattern {
        tracks = List.copyOf(tracks);
    }

    /**
     * One track of a {@link LightPattern}: a set of lights flashing together
     * with a shared sequence and color.
     *
     * @param lights The names of the lights this track controls (without the
     *               {@code light_} model part prefix).
     * @param color The RGB color the lights glow in while on.
     * @param phaseTicks Offset in ticks that delays this track's sequence.
     *                   Used to shift identical tracks relative to each other.
     * @param sequence The repeating on/off timeline.
     */
    public record Track(
        @NotNull Set<String> lights,
        int color,
        int phaseTicks,
        @NotNull FlashSequence sequence
    ) {
        public Track {
            lights = Set.copyOf(lights);
        }

        /**
         * Get the state this track gives its lights at the given time.
         *
         * @param timeTicks The time in ticks.
         * @return The state.
         */
        @NotNull
        public LightState getState(long timeTicks) {
            if (this.sequence.isOnAt(timeTicks - this.phaseTicks)) {
                return LightState.on(this.color);
            }
            return LightState.OFF;
        }
    }

    /**
     * Get the state of a light at the given time.
     * <p>
     * If several tracks control the same light, a track that currently has the
     * light on wins over tracks that have it off, and earlier tracks win over
     * later ones. This allows layering, for example a slow colored overlay on
     * top of a base pattern.
     *
     * @param lightName The name of the light (without the {@code light_} model
     *                  part prefix).
     * @param timeTicks The time in ticks.
     * @return The state of the light, or null if no track controls this light.
     */
    @Nullable
    public LightState getState(@NotNull String lightName, long timeTicks) {
        boolean controlled = false;
        for (Track track : this.tracks) {
            if (!track.lights().contains(lightName)) {
                continue;
            }
            controlled = true;
            LightState state = track.getState(timeTicks);
            if (state.on()) {
                return state;
            }
        }
        return controlled ? LightState.OFF : null;
    }

    /**
     * Get whether this pattern controls the given light.
     *
     * @param lightName The name of the light.
     * @return True if any track targets the light.
     */
    public boolean controls(@NotNull String lightName) {
        for (Track track : this.tracks) {
            if (track.lights().contains(lightName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the names of all lights controlled by this pattern.
     *
     * @return The set of light names.
     */
    @NotNull
    public Set<String> getLightNames() {
        Set<String> names = new HashSet<>();
        for (Track track : this.tracks) {
            names.addAll(track.lights());
        }
        return names;
    }

    /**
     * Parse a light pattern from JSON.
     * <p>
     * Format:
     * <pre>{@code
     * {
     *     "tracks": [
     *         {
     *             "lights": ["lightbar_1", "lightbar_2"],
     *             "color": "blue",
     *             "sequence": [2, 2, 2, 14],
     *             "phase": 10
     *         }
     *     ]
     * }
     * }</pre>
     * The {@code color} is either a named color (see {@link #parseColor}) or a
     * {@code "#RRGGBB"} hex string. {@code phase} is optional and defaults to 0.
     *
     * @param json The JSON object.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern fromJson(@NotNull JsonObject json) {
        JsonArray tracksJson = json.getAsJsonArray("tracks");
        if (tracksJson == null) {
            throw new IllegalArgumentException("Light pattern is missing \"tracks\".");
        }
        List<Track> tracks = new ArrayList<>(tracksJson.size());
        for (JsonElement trackElement : tracksJson) {
            JsonObject trackJson = trackElement.getAsJsonObject();

            JsonArray lightsJson = trackJson.getAsJsonArray("lights");
            if (lightsJson == null || lightsJson.isEmpty()) {
                throw new IllegalArgumentException("Light pattern track is missing \"lights\".");
            }
            Set<String> lights = new HashSet<>();
            for (JsonElement lightElement : lightsJson) {
                lights.add(lightElement.getAsString());
            }

            JsonElement colorJson = trackJson.get("color");
            if (colorJson == null) {
                throw new IllegalArgumentException("Light pattern track is missing \"color\".");
            }
            int color = parseColor(colorJson);

            JsonArray sequenceJson = trackJson.getAsJsonArray("sequence");
            if (sequenceJson == null) {
                throw new IllegalArgumentException("Light pattern track is missing \"sequence\".");
            }
            FlashSequence sequence = FlashSequence.fromJson(sequenceJson);

            int phase = trackJson.has("phase") ? trackJson.get("phase").getAsInt() : 0;

            tracks.add(new Track(lights, color, phase, sequence));
        }
        return new LightPattern(tracks);
    }

    /**
     * Named colors usable in light pattern JSON. Tuned for emergency and
     * warning lighting rather than being pure RGB primaries.
     */
    private static final Map<String, Integer> NAMED_COLORS = Map.of(
        "blue", 0x1E50FF,
        "red", 0xFF1E1E,
        "amber", 0xFFA000,
        "yellow", 0xFFE100,
        "orange", 0xFF7800,
        "white", 0xFFFFFF,
        "green", 0x28D728
    );

    /**
     * Parse a color from JSON. Either a named color like {@code "blue"} or
     * {@code "amber"}, a {@code "#RRGGBB"} hex string, or a raw integer.
     *
     * @param json The JSON element.
     * @return The RGB color.
     */
    public static int parseColor(@NotNull JsonElement json) {
        if (json.isJsonPrimitive() && ((JsonPrimitive) json).isNumber()) {
            return json.getAsInt();
        }
        String string = json.getAsString();
        if (string.startsWith("#")) {
            return Integer.parseInt(string.substring(1), 16);
        }
        Integer named = NAMED_COLORS.get(string.toLowerCase(Locale.ROOT));
        if (named == null) {
            throw new IllegalArgumentException("Unknown light color: " + string);
        }
        return named;
    }
}
