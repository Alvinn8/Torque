package ca.bkaw.torque.light;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * A repeating on/off timeline for a light.
 * <p>
 * The timeline is described as a list of alternating durations, measured in
 * ticks, where the first duration is an "on" duration. For example
 * {@code [2, 2, 2, 14]} means: on for 2 ticks, off for 2 ticks, on for 2
 * ticks, off for 14 ticks — a double flash followed by a pause. The sequence
 * then repeats, so the period is the sum of all durations (20 ticks in the
 * example, one double flash per second).
 * <p>
 * A sequence with a single duration, for example {@code [10]}, is always on
 * since there is no off duration.
 */
public final class FlashSequence {
    private final int[] durations;
    private final int period;

    private FlashSequence(int @NotNull [] durations) {
        if (durations.length == 0) {
            throw new IllegalArgumentException("A flash sequence needs at least one duration.");
        }
        int period = 0;
        for (int duration : durations) {
            if (duration <= 0) {
                throw new IllegalArgumentException(
                    "Flash sequence durations must be positive, got: " + Arrays.toString(durations)
                );
            }
            period += duration;
        }
        this.durations = durations;
        this.period = period;
    }

    /**
     * Create a flash sequence from alternating on/off durations, starting with
     * an "on" duration.
     *
     * @param durations The durations in ticks. All must be positive.
     * @return The flash sequence.
     */
    public static FlashSequence of(int @NotNull ... durations) {
        return new FlashSequence(durations.clone());
    }

    /**
     * Create a flash sequence that is always on.
     *
     * @return The flash sequence.
     */
    public static FlashSequence steady() {
        return new FlashSequence(new int[] { 1 });
    }

    /**
     * Parse a flash sequence from a JSON array of durations.
     * <p>
     * Example: {@code [2, 2, 2, 14]}.
     *
     * @param json The JSON array.
     * @return The flash sequence.
     */
    public static FlashSequence fromJson(@NotNull JsonArray json) {
        int[] durations = new int[json.size()];
        int i = 0;
        for (JsonElement element : json) {
            durations[i++] = element.getAsInt();
        }
        return new FlashSequence(durations);
    }

    /**
     * Get the period of this sequence, the number of ticks before the sequence
     * repeats.
     *
     * @return The period in ticks.
     */
    public int getPeriod() {
        return this.period;
    }

    /**
     * Get whether the light is on at the given time.
     *
     * @param timeTicks The time in ticks. May be any value, including negative;
     *                  the sequence repeats in both directions.
     * @return True if the light is on at this time.
     */
    public boolean isOnAt(long timeTicks) {
        int time = (int) Math.floorMod(timeTicks, this.period);
        int index = 0;
        while (time >= this.durations[index]) {
            time -= this.durations[index];
            index++;
        }
        // Even indexes are "on" durations, odd indexes are "off" durations.
        return index % 2 == 0;
    }

    @Override
    public String toString() {
        return "FlashSequence" + Arrays.toString(this.durations);
    }
}
