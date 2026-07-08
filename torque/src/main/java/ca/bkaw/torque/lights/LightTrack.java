package ca.bkaw.torque.lights;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * One flashing fixture (or group of fixtures that flash in unison) within a
 * {@link LightPattern}.
 *
 * @param lights The names of the lights this track controls. These are the light
 *               names from the model, tagged as {@code #light=<name>}.
 * @param color The glow color (0xRRGGBB) while the track is on.
 * @param phaseTicks Offset in ticks that delays the track's sequence. Two tracks
 *                   with the same sequence but different phases alternate.
 * @param sequence The on/off timeline the track repeats.
 */
public record LightTrack(
    @NotNull Set<String> lights,
    int color,
    int phaseTicks,
    @NotNull FlashSequence sequence
) {
    public LightTrack {
        lights = Set.copyOf(lights);
    }

    /**
     * Check whether this track is in an "on" phase at the given tick.
     *
     * @param tick The tick to evaluate at.
     * @return True if on.
     */
    public boolean isOnAt(long tick) {
        return this.sequence.isOnAt(tick - this.phaseTicks);
    }
}
