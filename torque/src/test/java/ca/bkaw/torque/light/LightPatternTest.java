package ca.bkaw.torque.light;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LightPatternTest {
    private static final int BLUE = 0x1E50FF;
    private static final int RED = 0xFF1E1E;

    @Test
    void alternatingSides() {
        // Left is on for the first 10 ticks, right for the next 10.
        LightPattern pattern = new LightPattern(List.of(
            new LightPattern.Track(Set.of("left"), BLUE, 0, FlashSequence.of(10, 10)),
            new LightPattern.Track(Set.of("right"), BLUE, 10, FlashSequence.of(10, 10))
        ));

        assertEquals(LightState.on(BLUE), pattern.getState("left", 0));
        assertEquals(LightState.OFF, pattern.getState("right", 0));

        assertEquals(LightState.OFF, pattern.getState("left", 10));
        assertEquals(LightState.on(BLUE), pattern.getState("right", 10));

        // Exactly one side is on at every tick.
        for (int t = 0; t < 40; t++) {
            boolean left = pattern.getState("left", t).on();
            boolean right = pattern.getState("right", t).on();
            assertTrue(left ^ right, "tick " + t);
        }
    }

    @Test
    void uncontrolledLightReturnsNull() {
        LightPattern pattern = new LightPattern(List.of(
            new LightPattern.Track(Set.of("beacon"), BLUE, 0, FlashSequence.steady())
        ));
        assertNull(pattern.getState("other", 0));
        assertTrue(pattern.controls("beacon"));
        assertFalse(pattern.controls("other"));
        assertEquals(Set.of("beacon"), pattern.getLightNames());
    }

    @Test
    void layeredTracksLitWins() {
        // A base track that has the light off half the time, with a red
        // overlay that fires during the off half.
        LightPattern pattern = new LightPattern(List.of(
            new LightPattern.Track(Set.of("beacon"), BLUE, 0, FlashSequence.of(10, 10)),
            new LightPattern.Track(Set.of("beacon"), RED, 10, FlashSequence.of(2, 18))
        ));
        // Base on: blue (earlier track wins).
        assertEquals(LightState.on(BLUE), pattern.getState("beacon", 0));
        // Base off, overlay on: red.
        assertEquals(LightState.on(RED), pattern.getState("beacon", 10));
        // Both off: off.
        assertEquals(LightState.OFF, pattern.getState("beacon", 15));
    }

    @Test
    void fromJson() {
        JsonObject json = JsonParser.parseString("""
            {
                "tracks": [
                    {
                        "lights": ["lightbar_1", "lightbar_2"],
                        "color": "blue",
                        "sequence": [2, 2, 2, 14]
                    },
                    {
                        "lights": ["lightbar_3"],
                        "color": "#FF0000",
                        "sequence": [5, 5],
                        "phase": 5
                    }
                ]
            }
            """).getAsJsonObject();
        LightPattern pattern = LightPattern.fromJson(json);

        assertEquals(2, pattern.tracks().size());
        assertEquals(Set.of("lightbar_1", "lightbar_2", "lightbar_3"), pattern.getLightNames());

        assertEquals(LightState.on(BLUE), pattern.getState("lightbar_1", 0));
        assertEquals(LightState.on(BLUE), pattern.getState("lightbar_2", 4));
        assertEquals(LightState.OFF, pattern.getState("lightbar_2", 2));

        // Phase 5 delays the on-phase of the third light.
        assertEquals(LightState.OFF, pattern.getState("lightbar_3", 0));
        assertEquals(LightState.on(0xFF0000), pattern.getState("lightbar_3", 5));
    }

    @Test
    void fromJsonRejectsMissingFields() {
        assertThrows(IllegalArgumentException.class, () ->
            LightPattern.fromJson(JsonParser.parseString("{}").getAsJsonObject()));
        assertThrows(IllegalArgumentException.class, () ->
            LightPattern.fromJson(JsonParser.parseString(
                "{\"tracks\": [{\"lights\": [\"a\"], \"color\": \"blue\"}]}"
            ).getAsJsonObject()));
    }

    @Test
    void parseColor() {
        assertEquals(0x1E50FF, LightPattern.parseColor(new JsonPrimitive("blue")));
        assertEquals(0xFFA000, LightPattern.parseColor(new JsonPrimitive("Amber")));
        assertEquals(0xABCDEF, LightPattern.parseColor(new JsonPrimitive("#ABCDEF")));
        assertEquals(1234, LightPattern.parseColor(new JsonPrimitive(1234)));
        assertThrows(IllegalArgumentException.class, () ->
            LightPattern.parseColor(new JsonPrimitive("no_such_color")));
    }
}
