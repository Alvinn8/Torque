package ca.bkaw.torque.lights;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

/**
 * A repeating on/off timeline, expressed as alternating on/off durations measured
 * in ticks, starting with an "on" duration.
 * <p>
 * For example, {@code [3, 3, 3, 11]} means on for 3 ticks, off for 3, on for 3
 * again and then off for 11 ticks before the sequence repeats from the start: a
 * classic emergency-light double flash. A duration of 0 is allowed and simply
 * skips that phase, which can be used to start a sequence with an off phase,
 * for example {@code [0, 10, 5, 5]}.
 */
public final class FlashSequence {
    private final int[] durations;
    private final int period;

    /**
     * Create a flash sequence.
     *
     * @param durations Alternating on/off durations in ticks, starting with on.
     */
    public FlashSequence(int @NotNull ... durations) {
        if (durations.length == 0) {
            throw new IllegalArgumentException("A flash sequence needs at least one duration.");
        }
        int period = 0;
        for (int duration : durations) {
            if (duration < 0) {
                throw new IllegalArgumentException("Flash sequence durations cannot be negative.");
            }
            period += duration;
        }
        if (period <= 0) {
            throw new IllegalArgumentException("A flash sequence must have a positive total duration.");
        }
        this.durations = durations.clone();
        this.period = period;
    }

    /**
     * Parse a flash sequence from a JSON array of integers.
     *
     * @param json The array of alternating on/off durations, starting with on.
     * @return The flash sequence.
     */
    @NotNull
    public static FlashSequence fromJson(@NotNull JsonArray json) {
        int[] durations = new int[json.size()];
        int i = 0;
        for (JsonElement element : json) {
            durations[i++] = element.getAsInt();
        }
        return new FlashSequence(durations);
    }

    /**
     * Check whether the sequence is in an "on" phase at the given tick.
     * <p>
     * The sequence loops forever, so any tick value is valid.
     *
     * @param tick The tick to evaluate at.
     * @return True if on.
     */
    public boolean isOnAt(long tick) {
        long t = Math.floorMod(tick, this.period);
        for (int i = 0; i < this.durations.length; i++) {
            t -= this.durations[i];
            if (t < 0) {
                // Even indexes are on phases, odd indexes are off phases.
                return i % 2 == 0;
            }
        }
        throw new AssertionError("tick outside period despite floorMod");
    }

    /**
     * Get the total duration of one loop of the sequence.
     *
     * @return The period in ticks.
     */
    public int getPeriod() {
        return this.period;
    }
}
