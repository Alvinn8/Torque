package ca.bkaw.torque.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainSamplerTest {
    private static final double Y_TOP = 10;
    private static final double Y_BOTTOM = -10;

    /**
     * Get the angle of the sampled normal from vertical, in degrees.
     */
    private double normalAngle(TerrainSampler sampler, double x, double z) {
        TerrainSampler.GroundSample sample = sampler.sample(x, z, Y_TOP, Y_BOTTOM);
        assertNotNull(sample);
        return Math.toDegrees(Math.acos(sample.normal().y()));
    }

    @Test
    void flatGroundIsExact() {
        TerrainSampler sampler = new TerrainSampler((x, z, yTop, yBottom) -> 7.25, 1.0);
        TerrainSampler.GroundSample sample = sampler.sample(3.7, -12.2, Y_TOP, Y_BOTTOM);
        assertNotNull(sample);
        // A weighted average of identical heights is exact, anywhere.
        assertEquals(7.25, sample.height(), 1e-9);
        // And the normal is exactly up.
        assertEquals(0.0, sample.normal().x(), 1e-9);
        assertEquals(1.0, sample.normal().y(), 1e-9);
        assertEquals(0.0, sample.normal().z(), 1e-9);
    }

    @Test
    void stepBecomesASmoothMonotonicRamp() {
        // A 1 m step at x = 0.
        GroundHeightFunction step = (x, z, yTop, yBottom) -> x < 0 ? 0 : 1;
        TerrainSampler sampler = new TerrainSampler(step, 1.0);

        // Far away from the step the surface is exact.
        assertEquals(0.0, sampler.sampleHeight(-2.5, 0, Y_TOP, Y_BOTTOM), 1e-9);
        assertEquals(1.0, sampler.sampleHeight(2.5, 0, Y_TOP, Y_BOTTOM), 1e-9);

        // The sample grid is symmetric around the step, so the middle is exact.
        assertEquals(0.5, sampler.sampleHeight(0, 0, Y_TOP, Y_BOTTOM), 1e-6);

        // Walking across the step, the height rises monotonically and continuously -
        // no jumps that would become force discontinuities.
        double previousHeight = sampler.sampleHeight(-2, 0, Y_TOP, Y_BOTTOM);
        for (double x = -2 + 0.01; x <= 2; x += 0.01) {
            double height = sampler.sampleHeight(x, 0, Y_TOP, Y_BOTTOM);
            assertTrue(height >= previousHeight - 1e-9, "height decreased at x = " + x);
            assertTrue(height - previousHeight <= 0.03, "height jumped at x = " + x);
            previousHeight = height;
        }

        // At the step the surface is a slope, not a wall.
        double angle = this.normalAngle(sampler, 0, 0);
        assertTrue(angle > 15 && angle < 60, "unexpected slope angle: " + angle);
    }

    @Test
    void kernelRadiusControlsSteepness() {
        GroundHeightFunction step = (x, z, yTop, yBottom) -> x < 0 ? 0 : 1;
        double angleNarrow = this.normalAngle(new TerrainSampler(step, 0.5), 0, 0);
        double angleMedium = this.normalAngle(new TerrainSampler(step, 1.0), 0, 0);
        double angleWide = this.normalAngle(new TerrainSampler(step, 1.5), 0, 0);

        // A smaller kernel radius keeps the step steeper; a larger one flattens it.
        // This is the dial that, compared against the tire friction angle atan(mu),
        // decides what terrain is climbable.
        assertTrue(angleNarrow > angleMedium, angleNarrow + " <= " + angleMedium);
        assertTrue(angleMedium > angleWide, angleMedium + " <= " + angleWide);

        // Sanity: with R = 0.5 the step is steeper than a mu = 1 tire can climb
        // (45 degrees); with R = 1.5 it is comfortably below.
        assertTrue(angleNarrow > 45, "narrow kernel angle: " + angleNarrow);
        assertTrue(angleWide < 45, "wide kernel angle: " + angleWide);
    }

    @Test
    void staircaseBecomesA45DegreeSlope() {
        // A staircase climbing 1 block per block in +x.
        GroundHeightFunction staircase = (x, z, yTop, yBottom) -> Math.floor(x);
        TerrainSampler sampler = new TerrainSampler(staircase, 1.0);

        for (double x : new double[]{ 0.5, 0.75, 1.0, 1.6 }) {
            // The smoothed staircase follows the average grade line h = x - 0.5.
            double height = sampler.sampleHeight(x, 0, Y_TOP, Y_BOTTOM);
            assertEquals(x - 0.5, height, 0.4, "height at x = " + x);

            // The kernel cannot change the average grade of a long slope, only
            // round its texture: the surface stays around 45 degrees.
            double angle = this.normalAngle(sampler, x, 0);
            assertTrue(angle > 30 && angle < 60, "angle at x = " + x + ": " + angle);
        }
    }

    @Test
    void slabsContributeTheirRealHeight() {
        // Half-slab ground everywhere.
        TerrainSampler sampler = new TerrainSampler((x, z, yTop, yBottom) -> 0.5, 1.0);
        assertEquals(0.5, sampler.sampleHeight(0.3, 0.9, Y_TOP, Y_BOTTOM), 1e-9);
    }

    @Test
    void noGroundMeansAirborne() {
        TerrainSampler sampler = new TerrainSampler((x, z, yTop, yBottom) -> Double.NaN, 1.0);
        assertTrue(Double.isNaN(sampler.sampleHeight(0, 0, Y_TOP, Y_BOTTOM)));
        assertNull(sampler.sample(0, 0, Y_TOP, Y_BOTTOM));
    }

    @Test
    void cliffEdgeDropsMissingSamplesGracefully() {
        // Ground for x < 0, a void beyond.
        GroundHeightFunction cliff = (x, z, yTop, yBottom) -> x < 0 ? 0 : Double.NaN;
        TerrainSampler sampler = new TerrainSampler(cliff, 1.0);

        // Standing near the edge: the missing samples drop out and the surface is
        // still the ground height, not dragged toward anything.
        TerrainSampler.GroundSample sample = sampler.sample(-0.5, 0, Y_TOP, Y_BOTTOM);
        assertNotNull(sample);
        assertEquals(0.0, sample.height(), 1e-9);

        // Fully past the edge: airborne.
        assertNull(sampler.sample(2.5, 0, Y_TOP, Y_BOTTOM));
    }
}
