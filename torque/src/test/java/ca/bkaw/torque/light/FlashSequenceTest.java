package ca.bkaw.torque.light;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSequenceTest {
    @Test
    void simpleBlink() {
        // On 2, off 3.
        FlashSequence sequence = FlashSequence.of(2, 3);
        assertEquals(5, sequence.getPeriod());
        assertTrue(sequence.isOnAt(0));
        assertTrue(sequence.isOnAt(1));
        assertFalse(sequence.isOnAt(2));
        assertFalse(sequence.isOnAt(3));
        assertFalse(sequence.isOnAt(4));
        // Repeats.
        assertTrue(sequence.isOnAt(5));
        assertFalse(sequence.isOnAt(7));
    }

    @Test
    void doubleFlash() {
        // On 2, off 2, on 2, off 14.
        FlashSequence sequence = FlashSequence.of(2, 2, 2, 14);
        assertEquals(20, sequence.getPeriod());
        assertTrue(sequence.isOnAt(0));
        assertFalse(sequence.isOnAt(2));
        assertTrue(sequence.isOnAt(4));
        assertTrue(sequence.isOnAt(5));
        for (int t = 6; t < 20; t++) {
            assertFalse(sequence.isOnAt(t), "tick " + t);
        }
        assertTrue(sequence.isOnAt(20));
    }

    @Test
    void steadyIsAlwaysOn() {
        FlashSequence sequence = FlashSequence.steady();
        for (int t = 0; t < 10; t++) {
            assertTrue(sequence.isOnAt(t));
        }
    }

    @Test
    void negativeTimeWrapsAround() {
        FlashSequence sequence = FlashSequence.of(2, 3);
        // Tick -1 is equivalent to tick 4 (off), tick -4 to tick 1 (on).
        assertFalse(sequence.isOnAt(-1));
        assertTrue(sequence.isOnAt(-4));
    }

    @Test
    void rejectsInvalidDurations() {
        assertThrows(IllegalArgumentException.class, FlashSequence::of);
        assertThrows(IllegalArgumentException.class, () -> FlashSequence.of(2, 0));
        assertThrows(IllegalArgumentException.class, () -> FlashSequence.of(-1));
    }

    @Test
    void fromJson() {
        FlashSequence sequence = FlashSequence.fromJson(
            JsonParser.parseString("[1, 4]").getAsJsonArray()
        );
        assertEquals(5, sequence.getPeriod());
        assertTrue(sequence.isOnAt(0));
        assertFalse(sequence.isOnAt(1));
    }
}
