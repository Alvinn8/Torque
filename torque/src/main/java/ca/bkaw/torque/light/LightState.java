package ca.bkaw.torque.light;

/**
 * The instantaneous state of a single light on a vehicle.
 *
 * @param on Whether the light is currently lit.
 * @param color The color of the light as an RGB integer (0xRRGGBB). Only
 *              meaningful while {@link #on} is true.
 */
public record LightState(boolean on, int color) {
    /**
     * The state of a light that is not lit.
     */
    public static final LightState OFF = new LightState(false, 0);

    /**
     * Create a lit state with the given color.
     *
     * @param color The RGB color.
     * @return The state.
     */
    public static LightState on(int color) {
        return new LightState(true, color);
    }
}
