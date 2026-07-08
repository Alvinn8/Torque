package ca.bkaw.torque.lights;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LightPatternTest {
    private static final int BLUE = 0x3366FF;
    private static final int AMBER = 0xFFB300;

    @Test
    public void alternatingTracksNeverOverlap() {
        // Same double flash sequence on both sides, right delayed by half the
        // period: the sides must alternate and never be on at the same time.
        LightPattern pattern = new LightPattern(List.of(
            new LightTrack(Set.of("left"), BLUE, 0, new FlashSequence(3, 3, 3, 11)),
            new LightTrack(Set.of("right"), BLUE, 10, new FlashSequence(3, 3, 3, 11))
        ));
        boolean leftSeen = false;
        boolean rightSeen = false;
        for (long tick = 0; tick < 200; tick++) {
            boolean left = pattern.getColor("left", tick) != null;
            boolean right = pattern.getColor("right", tick) != null;
            assertFalse(left && right, "both sides on at tick " + tick);
            leftSeen |= left;
            rightSeen |= right;
        }
        assertTrue(leftSeen);
        assertTrue(rightSeen);
    }

    @Test
    public void copPatternDriftsAndRepeatsAtLcm() {
        // Periods 20 and 24 drift against each other; the combined pattern
        // repeats only every lcm(20, 24) = 120 ticks.
        LightPattern pattern = new LightPattern(List.of(
            new LightTrack(Set.of("blue"), BLUE, 0, new FlashSequence(3, 3, 3, 11)),
            new LightTrack(Set.of("amber"), AMBER, 0, new FlashSequence(9, 15))
        ));
        boolean differsFromShorterCycle = false;
        for (long tick = 0; tick < 120; tick++) {
            assertEquals(pattern.getColor("blue", tick), pattern.getColor("blue", tick + 120));
            assertEquals(pattern.getColor("amber", tick), pattern.getColor("amber", tick + 120));
            if ((pattern.getColor("amber", tick) != null) != (pattern.getColor("amber", tick + 20) != null)) {
                differsFromShorterCycle = true;
            }
        }
        assertTrue(differsFromShorterCycle, "tracks with co-prime-ish periods should drift");
    }

    @Test
    public void controlsLightIncludesOffPhases() {
        LightPattern pattern = new LightPattern(List.of(
            new LightTrack(Set.of("beacon"), BLUE, 0, new FlashSequence(1, 19))
        ));
        assertTrue(pattern.controlsLight("beacon"));
        assertFalse(pattern.controlsLight("headlight"));
        assertEquals(Set.of("beacon"), pattern.getLights());
        // Off phase: still controlled, but no color.
        assertNull(pattern.getColor("beacon", 5));
        assertEquals(BLUE, pattern.getColor("beacon", 0));
        // Uncontrolled light is never on.
        assertNull(pattern.getColor("headlight", 0));
    }

    @Test
    public void fromJson() {
        JsonObject json = JsonParser.parseString("""
            {
                "tracks": [
                    {
                        "lights": ["beacon_blue_left"],
                        "color": "#3366FF",
                        "sequence": [3, 3, 3, 11]
                    },
                    {
                        "lights": ["beacon_blue_right"],
                        "color": "#3366FF",
                        "sequence": [3, 3, 3, 11],
                        "phase": 10
                    },
                    {
                        "lights": ["rear_amber_left", "rear_amber_right"],
                        "color": 16757504,
                        "sequence": [9, 15]
                    }
                ]
            }
            """).getAsJsonObject();
        LightPattern pattern = LightPattern.fromJson(json);
        assertEquals(3, pattern.getTracks().size());
        assertEquals(Set.of(
            "beacon_blue_left", "beacon_blue_right", "rear_amber_left", "rear_amber_right"
        ), pattern.getLights());
        assertEquals(BLUE, pattern.getTracks().get(0).color());
        assertEquals(10, pattern.getTracks().get(1).phaseTicks());
        assertEquals(0xFFB300, pattern.getTracks().get(2).color());
        assertNotNull(pattern.getColor("beacon_blue_left", 0));
        assertNull(pattern.getColor("beacon_blue_right", 0));
    }

    @Test
    public void fromJsonMissingKeys() {
        assertThrows(IllegalArgumentException.class, () ->
            LightPattern.fromJson(new JsonObject()));
        assertThrows(IllegalArgumentException.class, () ->
            LightPattern.fromJson(JsonParser.parseString(
                "{\"tracks\": [{\"lights\": [\"a\"]}]}").getAsJsonObject()));
    }

    @Test
    public void parseColor() {
        assertEquals(0x3366FF, LightPattern.parseColor(new JsonPrimitive("#3366FF")));
        assertEquals(0x3366FF, LightPattern.parseColor(new JsonPrimitive("3366FF")));
        assertEquals(0x3366FF, LightPattern.parseColor(new JsonPrimitive(0x3366FF)));
    }
}
