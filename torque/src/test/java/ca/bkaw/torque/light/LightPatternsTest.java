package ca.bkaw.torque.light;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LightPatternsTest {
    private static final int AMBER = 0xFFA000;

    @Test
    void steady() {
        LightPattern pattern = LightPatterns.steady(AMBER, Set.of("work_light"));
        for (int t = 0; t < 20; t++) {
            assertEquals(LightState.on(AMBER), pattern.getState("work_light", t));
        }
    }

    @Test
    void alternatingNeverBothSidesAtOnce() {
        LightPattern pattern = LightPatterns.alternating(
            AMBER, Set.of("left"), Set.of("right"), 2, 2
        );
        // Period: each side's turn is 8 ticks (2 flashes of 2 ticks + gaps).
        int period = 16;
        int litLeft = 0;
        int litRight = 0;
        for (int t = 0; t < 5 * period; t++) {
            boolean left = pattern.getState("left", t).on();
            boolean right = pattern.getState("right", t).on();
            assertTrue(!(left && right), "both sides lit at tick " + t);
            if (left) litLeft++;
            if (right) litRight++;
        }
        // Both sides flash, and equally much.
        assertTrue(litLeft > 0);
        assertEquals(litLeft, litRight);
    }

    @Test
    void sweepLitsExactlyOneLightAtATime() {
        List<String> lights = List.of("a", "b", "c", "d");
        LightPattern pattern = LightPatterns.sweep(AMBER, lights, 3);
        for (int t = 0; t < 30; t++) {
            int lit = 0;
            for (String light : lights) {
                if (pattern.getState(light, t).on()) {
                    lit++;
                }
            }
            assertEquals(1, lit, "tick " + t);
        }
        // The sweep travels in order.
        assertTrue(pattern.getState("a", 0).on());
        assertTrue(pattern.getState("b", 3).on());
        assertTrue(pattern.getState("c", 6).on());
        assertTrue(pattern.getState("d", 9).on());
        assertTrue(pattern.getState("a", 12).on());
    }

    @Test
    void chaoticStrobeControlsAllLightsAndDrifts() {
        List<String> lights = List.of("a", "b", "c");
        LightPattern pattern = LightPatterns.chaoticStrobe(AMBER, lights);
        assertEquals(Set.copyOf(lights), pattern.getLightNames());
        for (String light : lights) {
            int lit = 0;
            for (int t = 0; t < 200; t++) {
                if (pattern.getState(light, t).on()) {
                    lit++;
                }
            }
            assertTrue(lit > 0, light + " never lit");
        }
        // The lights have different periods, so the joint pattern must not
        // repeat with the period of a single light (13 ticks for "a").
        boolean drifts = false;
        for (int t = 0; t < 200; t++) {
            for (String light : lights) {
                if (pattern.getState(light, t).on() != pattern.getState(light, t + 13).on()) {
                    drifts = true;
                }
            }
        }
        assertTrue(drifts);
    }
}
