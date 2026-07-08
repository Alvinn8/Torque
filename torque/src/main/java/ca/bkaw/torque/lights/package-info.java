/**
 * Data-driven flashing light patterns for emergency and warning lights: hazard
 * lights, amber warning beacons, police lightbars, and similar.
 * <p>
 * Like the {@code physics} package, this package is deliberately decoupled from
 * {@code Vehicle}/{@code VehicleComponent} so the timing logic can be unit
 * tested. {@link ca.bkaw.torque.lights.LightPattern} documents the JSON format
 * and how to compose tracks into a convincing chaotic pattern; the
 * {@code SignalLightsComponent} connects patterns to a vehicle's light parts.
 */
package ca.bkaw.torque.lights;
