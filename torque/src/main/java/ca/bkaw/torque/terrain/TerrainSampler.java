package ca.bkaw.torque.terrain;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

/**
 * Samples a smooth, continuous terrain surface from discrete blocky geometry.
 * <p>
 * Block terrain is made of one-meter steps that no realistic wheel could climb, so
 * wheels do not ride on the raw block geometry. Instead, this sampler interprets the
 * blocks as a rendering of an underlying smooth surface: the height at any continuous
 * point is a kernel-weighted average of the ground heights sampled on a fixed grid
 * around it (Nadaraya-Watson kernel regression / Shepard interpolation):
 * <pre>
 *     h(x, z) = sum( w(d_i) * h_i ) / sum( w(d_i) )
 * </pre>
 * where {@code d_i} is the horizontal distance from the query point to sample point
 * {@code i}. The sample points are anchored to the block grid ({@link #SAMPLE_SPACING}
 * apart, 2x2 per block so slabs and stairs contribute their real shape), and the
 * kernel is a quartic bump that falls smoothly to zero at the kernel radius:
 * <pre>
 *     w(d) = (1 - (d/R)^2)^2    for d &lt; R, else 0
 * </pre>
 * Because the samples are fixed and the kernel is smooth (zero value <em>and</em>
 * slope at its edge), the result varies smoothly as the query point moves, and so
 * does the surface normal - which matters because the normal becomes a force
 * direction, and kinks in the height would be jumps in the force.
 * <p>
 * The kernel radius is the "terrain interpretation" dial, with interpretable units: a
 * single 1-block step smooths into a ramp roughly {@code 2R} long, i.e. a grade of
 * about {@code atan(1/(2R))} - 45 degrees at R = 0.5, ~34 degrees at R = 0.75,
 * ~22 degrees at R = 1.25. Combined with the tire friction limit (a wheel-driven
 * vehicle cannot climb slopes steeper than about {@code atan(mu)}), the radius
 * directly decides what is drivable: single steps become steep-but-climbable ramps,
 * while multi-block walls stay too steep and remain obstacles.
 * <p>
 * The physics stays exact - the smoothing reinterprets the world's <em>geometry</em>,
 * it does not skip any forces.
 */
public class TerrainSampler {
    /**
     * Horizontal spacing of the grid-anchored sample points. Sample points lie at
     * {@code n * 0.5 + 0.25} in world coordinates on both horizontal axes, giving
     * 2x2 sample points per block so sub-block shapes (slabs, stairs) contribute
     * their real height.
     */
    public static final double SAMPLE_SPACING = 0.5;

    /**
     * The offset of the sampling grid from block coordinates.
     */
    public static final double SAMPLE_OFFSET = 0.25;

    /**
     * The step used for the finite-difference normal estimation. Unit: meter.
     */
    private static final double NORMAL_EPSILON = 0.1;

    private final GroundHeightFunction ground;
    private final double kernelRadius;

    /**
     * A smoothed terrain sample: the surface height and the surface normal at a point.
     *
     * @param height The smoothed surface height. Unit: meter.
     * @param normal The smoothed surface normal (unit vector, pointing up out of the ground).
     */
    public record GroundSample(double height, Vector3d normal) {}

    /**
     * @param ground The ground height source (in game: {@code world::getGroundHeight}).
     * @param kernelRadius The kernel radius R. Larger values smooth the terrain more,
     *                     making steps gentler to drive over. Unit: meter.
     */
    public TerrainSampler(GroundHeightFunction ground, double kernelRadius) {
        this.ground = ground;
        this.kernelRadius = kernelRadius;
    }

    public double getKernelRadius() {
        return this.kernelRadius;
    }

    /**
     * Sample the smoothed terrain surface at a horizontal position.
     *
     * @param x The x coordinate of the query point. Unit: meter.
     * @param z The z coordinate of the query point. Unit: meter.
     * @param yTop The top of the vertical scan window, usually just above the wheel
     *             hub. Surfaces above this are clamped to it (they are obstacles, not
     *             terrain). Unit: meter.
     * @param yBottom The bottom of the vertical scan window, usually the lowest point
     *                the suspension can reach. Unit: meter.
     * @return The smoothed height and normal, or null if there is no ground within
     * the window (airborne).
     */
    @Nullable
    public GroundSample sample(double x, double z, double yTop, double yBottom) {
        double height = this.sampleHeight(x, z, yTop, yBottom);
        if (Double.isNaN(height)) {
            return null;
        }

        // Estimate the normal with central finite differences on the smoothed
        // height field. At cliff edges a neighboring sample may be over the void;
        // fall back to the center height there, which flattens the normal rather
        // than producing NaN.
        double hxPlus = this.sampleHeightOr(x + NORMAL_EPSILON, z, yTop, yBottom, height);
        double hxMinus = this.sampleHeightOr(x - NORMAL_EPSILON, z, yTop, yBottom, height);
        double hzPlus = this.sampleHeightOr(x, z + NORMAL_EPSILON, yTop, yBottom, height);
        double hzMinus = this.sampleHeightOr(x, z - NORMAL_EPSILON, yTop, yBottom, height);

        Vector3d normal = new Vector3d(
            -(hxPlus - hxMinus) / (2 * NORMAL_EPSILON),
            1.0,
            -(hzPlus - hzMinus) / (2 * NORMAL_EPSILON)
        ).normalize();

        return new GroundSample(height, normal);
    }

    /**
     * Sample only the smoothed height at a horizontal position.
     *
     * @return The smoothed height, or {@link Double#NaN} if there is no ground within
     * the window.
     */
    public double sampleHeight(double x, double z, double yTop, double yBottom) {
        double radiusSquared = this.kernelRadius * this.kernelRadius;

        // Indexes of the grid-anchored sample points within the kernel radius.
        // Sample points are at n * SAMPLE_SPACING + SAMPLE_OFFSET.
        int minIndexX = (int) Math.ceil((x - this.kernelRadius - SAMPLE_OFFSET) / SAMPLE_SPACING);
        int maxIndexX = (int) Math.floor((x + this.kernelRadius - SAMPLE_OFFSET) / SAMPLE_SPACING);
        int minIndexZ = (int) Math.ceil((z - this.kernelRadius - SAMPLE_OFFSET) / SAMPLE_SPACING);
        int maxIndexZ = (int) Math.floor((z + this.kernelRadius - SAMPLE_OFFSET) / SAMPLE_SPACING);

        double weightSum = 0;
        double heightSum = 0;
        for (int indexX = minIndexX; indexX <= maxIndexX; indexX++) {
            double sampleX = indexX * SAMPLE_SPACING + SAMPLE_OFFSET;
            double dx = sampleX - x;
            for (int indexZ = minIndexZ; indexZ <= maxIndexZ; indexZ++) {
                double sampleZ = indexZ * SAMPLE_SPACING + SAMPLE_OFFSET;
                double dz = sampleZ - z;
                double distanceSquared = dx * dx + dz * dz;
                if (distanceSquared >= radiusSquared) {
                    continue;
                }
                double sampleHeight = this.ground.getGroundHeight(sampleX, sampleZ, yTop, yBottom);
                if (Double.isNaN(sampleHeight)) {
                    // No ground at this sample point (a hole, or over a cliff edge).
                    // The sample simply drops out; the normalization by the weight
                    // sum handles the missing contribution.
                    continue;
                }
                // Quartic kernel: smooth, and both the value and the slope reach
                // zero at the radius, so samples entering/leaving the support as
                // the query point moves never cause a discontinuity.
                double a = 1.0 - distanceSquared / radiusSquared;
                double weight = a * a;
                weightSum += weight;
                heightSum += weight * sampleHeight;
            }
        }

        if (weightSum < 1e-9) {
            return Double.NaN;
        }
        return heightSum / weightSum;
    }

    private double sampleHeightOr(double x, double z, double yTop, double yBottom, double fallback) {
        double height = this.sampleHeight(x, z, yTop, yBottom);
        return Double.isNaN(height) ? fallback : height;
    }
}
