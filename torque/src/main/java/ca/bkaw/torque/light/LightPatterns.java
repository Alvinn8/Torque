package ca.bkaw.torque.light;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builders for common warning and emergency light patterns.
 * <p>
 * These cover the typical building blocks of emergency lighting:
 * <ul>
 *     <li>{@link #steady} — lights that are simply on (rear ambers, work lights).</li>
 *     <li>{@link #alternating} — the classic "wig-wag" where two sides take
 *     turns, used for police blue lights and stop signals.</li>
 *     <li>{@link #sweep} — a light chasing across a lightbar, used for
 *     directional amber arrows on road-work and blocking vehicles.</li>
 *     <li>{@link #chaoticStrobe} — every light triple-flashing with its own
 *     period so the flashes constantly drift against each other, giving the
 *     hectic accident-scene look.</li>
 * </ul>
 * Patterns are plain data ({@link LightPattern}); these builders can be
 * composed by concatenating their {@link LightPattern#tracks() tracks} into a
 * new pattern.
 */
public final class LightPatterns {
    private LightPatterns() {}

    /**
     * Periods used by {@link #chaoticStrobe} to keep lights out of sync with
     * each other. Chosen to be pairwise coprime so any two lights only line up
     * once per several seconds.
     */
    private static final int[] DRIFTING_PERIODS = { 13, 15, 17, 19, 23, 29 };

    /**
     * A pattern where the lights are constantly on.
     *
     * @param color The RGB color.
     * @param lights The light names.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern steady(int color, @NotNull Set<String> lights) {
        return new LightPattern(List.of(
            new LightPattern.Track(lights, color, 0, FlashSequence.steady())
        ));
    }

    /**
     * An alternating "wig-wag" pattern: side A fires a burst of flashes, then
     * side B, and so on. With {@code flashesPerSide = 2} and
     * {@code flashTicks = 2} each side does a quick double flash before
     * handing over — the typical modern police flash.
     *
     * @param color The RGB color.
     * @param sideA The light names of the first side.
     * @param sideB The light names of the second side.
     * @param flashesPerSide How many flashes each side fires per turn.
     * @param flashTicks The duration in ticks of each flash and each gap
     *                   within a burst.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern alternating(
        int color,
        @NotNull Set<String> sideA,
        @NotNull Set<String> sideB,
        int flashesPerSide,
        int flashTicks
    ) {
        // One side's burst: on/off repeated, where the last "off" is extended
        // to cover the other side's entire turn.
        int burstTicks = (2 * flashesPerSide) * flashTicks;
        int[] durations = new int[2 * flashesPerSide];
        for (int i = 0; i < durations.length; i++) {
            durations[i] = flashTicks;
        }
        durations[durations.length - 1] = flashTicks + burstTicks;
        FlashSequence sequence = FlashSequence.of(durations);
        return new LightPattern(List.of(
            new LightPattern.Track(sideA, color, 0, sequence),
            new LightPattern.Track(sideB, color, burstTicks, sequence)
        ));
    }

    /**
     * A sweep pattern where one light at a time is lit, chasing across the
     * given lights in order and wrapping around. Used for directional arrows
     * and attention-grabbing lightbar sweeps.
     *
     * @param color The RGB color.
     * @param lightsInOrder The light names in the order the sweep travels.
     * @param stepTicks How many ticks each light stays lit.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern sweep(int color, @NotNull List<String> lightsInOrder, int stepTicks) {
        int period = lightsInOrder.size() * stepTicks;
        List<LightPattern.Track> tracks = new ArrayList<>(lightsInOrder.size());
        for (int i = 0; i < lightsInOrder.size(); i++) {
            FlashSequence sequence = lightsInOrder.size() == 1
                ? FlashSequence.steady()
                : FlashSequence.of(stepTicks, period - stepTicks);
            tracks.add(new LightPattern.Track(
                Set.of(lightsInOrder.get(i)), color, i * stepTicks, sequence
            ));
        }
        return new LightPattern(tracks);
    }

    /**
     * A deliberately unsynchronized strobe: every light triple-flashes, but
     * each light gets its own period and phase from a set of pairwise coprime
     * periods, so the lights continuously drift in and out of sync. This is
     * what gives the chaotic feel of several emergency vehicles at an accident
     * scene, even on a single vehicle.
     *
     * @param color The RGB color.
     * @param lightsInOrder The light names. Order determines which period each
     *                      light gets, so the same input always produces the
     *                      same pattern.
     * @return The pattern.
     */
    @NotNull
    public static LightPattern chaoticStrobe(int color, @NotNull List<String> lightsInOrder) {
        List<LightPattern.Track> tracks = new ArrayList<>(lightsInOrder.size());
        for (int i = 0; i < lightsInOrder.size(); i++) {
            int period = DRIFTING_PERIODS[i % DRIFTING_PERIODS.length];
            // Triple flash (on 1, off 1, on 1, off 1, on 1) then rest for the
            // remainder of the period.
            FlashSequence sequence = FlashSequence.of(1, 1, 1, 1, 1, period - 5);
            tracks.add(new LightPattern.Track(
                Set.of(lightsInOrder.get(i)), color, i * 3, sequence
            ));
        }
        return new LightPattern(tracks);
    }
}
