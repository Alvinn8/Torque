package ca.bkaw.torque.terrain;

/**
 * A function that finds the ground surface height at a horizontal position.
 * <p>
 * This is the input to {@link TerrainSampler}. In the game it is backed by
 * {@link ca.bkaw.torque.platform.World#getGroundHeight}, which scans block collision
 * shapes; in unit tests it can be any lambda describing a synthetic terrain.
 */
@FunctionalInterface
public interface GroundHeightFunction {
    /**
     * Find the top surface of the first collidable geometry at the given horizontal
     * position, scanning downward from {@code yTop} to {@code yBottom}.
     * <p>
     * If geometry is already solid at {@code yTop}, the surface is clamped to
     * {@code yTop}. This clamp is what separates <em>terrain</em> from
     * <em>obstacles</em>: a surface within the scan window contributes its real
     * height to the smoothed terrain, while anything taller only ever contributes
     * "at least {@code yTop}" and must be handled as a collision instead.
     *
     * @param x The x coordinate. Unit: meter.
     * @param z The z coordinate. Unit: meter.
     * @param yTop The top of the scan window. Unit: meter.
     * @param yBottom The bottom of the scan window. Unit: meter.
     * @return The surface y, clamped to at most {@code yTop}, or {@link Double#NaN}
     * if there is no surface within the window.
     */
    double getGroundHeight(double x, double z, double yTop, double yBottom);
}
