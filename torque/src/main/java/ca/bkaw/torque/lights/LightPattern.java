package ca.bkaw.torque.lights;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A deterministic, looping light show: a set of {@link LightTrack}s that each
 * flash a group of lights with their own timing.
 * <p>
 * Real emergency lightbars get their chaotic feel not from randomness but from
 * several simple periodic fixtures that are not synchronized with each other.
 * To reproduce that:
 * <ul>
 *     <li>Use short bursts ({@code [3, 3, 3, 11]}) rather than even blinking.</li>
 *     <li>Give left/right tracks the same sequence but different {@code phase}
 *     so they alternate.</li>
 *     <li>Give different fixtures periods that share no large common factor
 *     (for example 20 and 24 ticks) so they slowly drift in and out of phase
 *     and the combined pattern only repeats every least-common-multiple
 *     ticks.</li>
 * </ul>
 * Because the evaluation is a pure function of the tick counter, the pattern
 * stays deterministic: all players see the same show, and it survives
 * serialization without extra state.
 */
public final class LightPattern {
    private final List<LightTrack> tracks;
    private final Set<String> lights;

    public LightPattern(@NotNull List<LightTrack> tracks) {
        this.tracks = List.copyOf(tracks);
        Set<String> lights = new HashSet<>();
        for (LightTrack track : this.tracks) {
            lights.addAll(track.lights());
        }
        this.lights = Set.copyOf(lights);
    }

    /**
     * Parse a light pattern from JSON.
     * <p>
     * Expected format:
     * <pre>{@code
     * {
     *     "tracks": [
     *         {
     *             "lights": ["beacon_blue_left"],
     *             "color": "#3366FF",
     *             "sequence": [3, 3, 3, 11],
     *             "phase": 0
     *         }
     *     ]
     * }
     * }</pre>
     * The {@code color} (default white) and {@code phase} (default 0) keys are
     * optional. The color can be a {@code "#RRGGBB"} string or an integer.
     *
     * @param json The JSON object.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern fromJson(@NotNull JsonObject json) {
        if (!json.has("tracks")) {
            throw new IllegalArgumentException("Light pattern is missing required key: tracks");
        }
        List<LightTrack> tracks = new ArrayList<>();
        for (JsonElement trackElement : json.getAsJsonArray("tracks")) {
            JsonObject trackJson = trackElement.getAsJsonObject();
            if (!trackJson.has("lights") || !trackJson.has("sequence")) {
                throw new IllegalArgumentException("Light pattern track is missing required key: lights or sequence");
            }
            Set<String> lights = new LinkedHashSet<>();
            for (JsonElement light : trackJson.getAsJsonArray("lights")) {
                lights.add(light.getAsString());
            }
            int color = trackJson.has("color") ? parseColor(trackJson.get("color")) : 0xFFFFFF;
            int phase = trackJson.has("phase") ? trackJson.get("phase").getAsInt() : 0;
            FlashSequence sequence = FlashSequence.fromJson(trackJson.getAsJsonArray("sequence"));
            tracks.add(new LightTrack(lights, color, phase, sequence));
        }
        return new LightPattern(tracks);
    }

    /**
     * Parse a color from JSON, either a {@code "#RRGGBB"} string or an integer.
     *
     * @param json The JSON element.
     * @return The color as 0xRRGGBB.
     */
    public static int parseColor(@NotNull JsonElement json) {
        if (json instanceof JsonPrimitive primitive && primitive.isString()) {
            String string = primitive.getAsString();
            if (string.startsWith("#")) {
                string = string.substring(1);
            }
            return Integer.parseInt(string, 16);
        }
        return json.getAsInt();
    }

    /**
     * Check whether this pattern controls the given light. A pattern controls a
     * light if any of its tracks reference it, and while the pattern is active it
     * fully owns those lights, including during their off phases.
     *
     * @param lightName The light name.
     * @return True if the pattern controls the light.
     */
    public boolean controlsLight(@NotNull String lightName) {
        return this.lights.contains(lightName);
    }

    /**
     * Get the color the light should glow with at the given tick.
     *
     * @param lightName The light name.
     * @param tick The tick to evaluate at.
     * @return The glow color (0xRRGGBB), or null if the light is off.
     */
    @Nullable
    public Integer getColor(@NotNull String lightName, long tick) {
        for (LightTrack track : this.tracks) {
            if (track.lights().contains(lightName) && track.isOnAt(tick)) {
                return track.color();
            }
        }
        return null;
    }

    /**
     * Get the names of all lights referenced by this pattern's tracks.
     *
     * @return The light names.
     */
    @NotNull
    public Set<String> getLights() {
        return this.lights;
    }

    /**
     * Get the tracks of this pattern.
     *
     * @return The tracks.
     */
    @NotNull
    public List<LightTrack> getTracks() {
        return this.tracks;
    }
}
