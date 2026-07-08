package ca.bkaw.torque.lights;

import com.google.gson.JsonArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlashSequenceTest {

    @Test
    public void evenBlink() {
        FlashSequence sequence = new FlashSequence(10, 10);
        assertEquals(20, sequence.getPeriod());
        for (int tick = 0; tick < 10; tick++) {
            assertTrue(sequence.isOnAt(tick), "tick " + tick);
        }
        for (int tick = 10; tick < 20; tick++) {
            assertFalse(sequence.isOnAt(tick), "tick " + tick);
        }
    }

    @Test
    public void loops() {
        FlashSequence sequence = new FlashSequence(3, 7);
        for (long offset : new long[] { 0, 10, 100, 12345 * 10L }) {
            assertTrue(sequence.isOnAt(offset));
            assertTrue(sequence.isOnAt(offset + 2));
            assertFalse(sequence.isOnAt(offset + 3));
            assertFalse(sequence.isOnAt(offset + 9));
        }
    }

    @Test
    public void doubleFlash() {
        // on 3, off 3, on 3, off 11
        FlashSequence sequence = new FlashSequence(3, 3, 3, 11);
        assertEquals(20, sequence.getPeriod());
        assertTrue(sequence.isOnAt(0));
        assertTrue(sequence.isOnAt(2));
        assertFalse(sequence.isOnAt(3));
        assertFalse(sequence.isOnAt(5));
        assertTrue(sequence.isOnAt(6));
        assertTrue(sequence.isOnAt(8));
        assertFalse(sequence.isOnAt(9));
        assertFalse(sequence.isOnAt(19));
        assertTrue(sequence.isOnAt(20)); // loops
    }

    @Test
    public void negativeTicksAreValid() {
        // Phase offsets can make the evaluated tick negative.
        FlashSequence sequence = new FlashSequence(5, 5);
        assertTrue(sequence.isOnAt(-10));
        assertFalse(sequence.isOnAt(-5));
        assertTrue(sequence.isOnAt(-6 - 10));
    }

    @Test
    public void zeroDurationSkipsPhase() {
        // Starts with a zero-length on phase, so the sequence starts off.
        FlashSequence sequence = new FlashSequence(0, 10, 5, 5);
        assertFalse(sequence.isOnAt(0));
        assertFalse(sequence.isOnAt(9));
        assertTrue(sequence.isOnAt(10));
        assertTrue(sequence.isOnAt(14));
        assertFalse(sequence.isOnAt(15));
    }

    @Test
    public void invalidSequences() {
        assertThrows(IllegalArgumentException.class, FlashSequence::new);
        assertThrows(IllegalArgumentException.class, () -> new FlashSequence(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new FlashSequence(5, -1));
    }

    @Test
    public void fromJson() {
        JsonArray json = new JsonArray();
        json.add(2);
        json.add(18);
        FlashSequence sequence = FlashSequence.fromJson(json);
        assertEquals(20, sequence.getPeriod());
        assertTrue(sequence.isOnAt(1));
        assertFalse(sequence.isOnAt(2));
    }
}
