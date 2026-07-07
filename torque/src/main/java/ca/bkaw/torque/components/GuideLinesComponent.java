package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.tags.WheelTags;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renders "guide lines" on the ground that show where the sides of the vehicle
 * will travel if the current steering wheel angle is kept, like the guidance
 * lines on a car's backup camera.
 * <p>
 * The lines follow the same Ackermann steering geometry that {@link WheelComponent}
 * steers the wheels with: every point on the vehicle travels on a circle around
 * the turning center, so the predicted path is the sides of the vehicle rotated
 * rigidly around that center. The lines are shown ahead of the vehicle when
 * driving forward and behind it when reversing.
 * <p>
 * This component only computes the path; the display entities are managed by
 * {@link ca.bkaw.torque.render.VehicleRenderer}.
 */
public class GuideLinesComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.builder(
            new Identifier("torque", "guide_lines")
        )
        .configParser(GuideLinesComponent::parseConfig)
        .create(GuideLinesComponent::new);

    /**
     * Items used for the line segments, by distance along the path. Near segments
     * are red, then yellow, then green, like backup camera distance bands.
     */
    private static final Identifier NEAR_ITEM = new Identifier("minecraft", "red_concrete");
    private static final Identifier MIDDLE_ITEM = new Identifier("minecraft", "yellow_concrete");
    private static final Identifier FAR_ITEM = new Identifier("minecraft", "lime_concrete");
    private static final double NEAR_DISTANCE = 2.0; // unit: meters
    private static final double MIDDLE_DISTANCE = 4.0; // unit: meters

    /**
     * Distance along the path between sampled points. Each pair of consecutive
     * points becomes one rendered segment.
     */
    private static final double SAMPLE_STEP = 0.8; // unit: meters

    /**
     * Distance along the path where the lines start, so they do not clip into the
     * vehicle body.
     */
    private static final double START_OFFSET = 0.6; // unit: meters

    /**
     * How far above the wheel contact plane the lines hover, to avoid z-fighting
     * with the ground.
     */
    private static final double GROUND_CLEARANCE = 0.05; // unit: meters

    /**
     * The maximum angle of the turning circle the lines may cover, so that the
     * lines do not wrap around into a full circle at full steering lock.
     */
    private static final double MAX_ARC_ANGLE = 3.5; // unit: radians

    /**
     * Above this speed the direction of travel decides whether the lines are shown
     * in front of or behind the vehicle; below it the driver's pressed keys do.
     */
    private static final double MIN_MOVEMENT_SPEED = 0.5; // unit: meters/second

    public record Config(double length, double width, double lineWidth) {}

    private static Config parseConfig(JsonObject json) {
        double length = json.has("length") ? json.get("length").getAsDouble() : 8.0;
        double width = json.has("width") ? json.get("width").getAsDouble() : Double.NaN;
        double lineWidth = json.has("line_width") ? json.get("line_width").getAsDouble() : 0.1;
        return new Config(length, width, lineWidth);
    }

    /**
     * One straight piece of a guide line, in world coordinates.
     *
     * @param start The start of the segment. Unit: meters, world coordinates.
     * @param end The end of the segment. Unit: meters, world coordinates.
     * @param width How wide the rendered line is. Unit: meters.
     * @param item The item whose model to render the segment with.
     */
    public record Segment(Vector3dc start, Vector3dc end, double width, Identifier item) {}

    private final Config config;

    // Vehicle extents derived from the wheel contact patches, in model coordinates.
    private final boolean hasWheelGeometry;
    private final double frontZ; // In model coordinates +Z is backwards
    private final double backZ;
    private final double groundY;
    private final double trackHalfWidth;

    private final List<Segment> segments = new ArrayList<>();

    public GuideLinesComponent(Vehicle vehicle, Config config, DataInput data) {
        this.config = config;

        List<WheelTags.Wheel> wheels = vehicle.getType().model()
            .getTagData(WheelTags.class).orElse(List.of());
        this.hasWheelGeometry = !wheels.isEmpty();
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        for (WheelTags.Wheel wheel : wheels) {
            Vector3d contactPatch = wheel.contactPatch();
            minX = Math.min(minX, contactPatch.x);
            maxX = Math.max(maxX, contactPatch.x);
            minZ = Math.min(minZ, contactPatch.z);
            maxZ = Math.max(maxZ, contactPatch.z);
            minY = Math.min(minY, contactPatch.y);
        }
        this.frontZ = minZ;
        this.backZ = maxZ;
        this.groundY = minY;
        this.trackHalfWidth = (maxX - minX) / 2;
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        this.segments.clear();
        if (!this.hasWheelGeometry) {
            return;
        }
        Input driverInput = vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);
        if (driverInput == null) {
            // Only show the guide lines while someone is driving.
            return;
        }
        RigidBodyComponent rbc = vehicle.getComponent(RigidBodyComponent.class).orElse(null);
        WheelComponent wheelComponent = vehicle.getComponent(WheelComponent.class).orElse(null);
        if (rbc == null || wheelComponent == null) {
            return;
        }

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);
        double turningRadius = wheelComponent.getTurningRadius(steeringWheelAngle);

        Quaterniondc orientation = new Quaterniond(rbc.getOrientation());
        Vector3d forward = new Vector3d(0, 0, -1).rotate(orientation);
        double signedSpeed = rbc.getVelocity().dot(forward);
        int direction;
        if (Math.abs(signedSpeed) > MIN_MOVEMENT_SPEED) {
            direction = signedSpeed > 0 ? 1 : -1;
        } else if (driverInput.backward && !driverInput.forward) {
            direction = -1;
        } else {
            direction = 1;
        }

        double halfWidth;
        if (!Double.isNaN(this.config.width())) {
            halfWidth = this.config.width() / 2;
        } else {
            halfWidth = vehicle.getComponent(HitboxComponent.class)
                .map(hitbox -> hitbox.getConfig().width() / 2.0)
                .orElse(this.trackHalfWidth + 0.2);
        }

        double length = this.config.length();
        if (Double.isFinite(turningRadius)) {
            // Do not let the lines wrap around the turning circle at full lock.
            length = Math.min(length, Math.abs(turningRadius) * MAX_ARC_ANGLE);
        }

        // The lines start at the leading axle and extend in the direction of travel.
        double startZ = direction > 0 ? this.frontZ : this.backZ;
        double y = this.groundY + GROUND_CLEARANCE;

        for (int side = -1; side <= 1; side += 2) {
            Vector3d base = new Vector3d(side * halfWidth, y, startZ);
            Vector3d previous = this.toWorld(
                this.samplePathPoint(base, START_OFFSET, direction, turningRadius),
                vehicle, rbc.getPosition(), orientation
            );
            for (double s = START_OFFSET + SAMPLE_STEP; s <= length; s += SAMPLE_STEP) {
                Vector3d point = this.toWorld(
                    this.samplePathPoint(base, s, direction, turningRadius),
                    vehicle, rbc.getPosition(), orientation
                );
                this.segments.add(new Segment(
                    previous, point,
                    this.config.lineWidth(),
                    itemForDistance(s - SAMPLE_STEP)
                ));
                previous = point;
            }
        }
    }

    /**
     * Get the point, in model coordinates, that the given point on the vehicle will
     * be at after traveling the given distance along the predicted path.
     * <p>
     * Every point on the vehicle travels on a circle around the turning center, so
     * the prediction is a rigid rotation around the center by the arc angle that
     * corresponds to the traveled distance.
     *
     * @param base The point on the vehicle. Unit: meters, model coordinates.
     * @param distance The distance traveled along the path. Unit: meters.
     * @param direction 1 when traveling forward, -1 when reversing.
     * @param turningRadius The signed turning radius from
     * {@link WheelComponent#getTurningRadius}. Unit: meters.
     * @return The predicted point. Unit: meters, model coordinates.
     */
    private Vector3d samplePathPoint(Vector3dc base, double distance, int direction, double turningRadius) {
        if (!Double.isFinite(turningRadius)) {
            // Driving straight. In model coordinates -Z is forward.
            return new Vector3d(base).add(0, 0, -direction * distance);
        }
        double arcAngle = -direction * distance / turningRadius;
        return new Vector3d(base)
            .sub(turningRadius, 0, this.backZ)
            .rotateY(arcAngle)
            .add(turningRadius, 0, this.backZ);
    }

    private Vector3d toWorld(Vector3d modelPoint, Vehicle vehicle, Vector3dc vehiclePosition, Quaterniondc orientation) {
        return modelPoint
            .add(vehicle.getType().model().getPrimary().translation())
            .rotate(orientation)
            .add(vehiclePosition);
    }

    private static Identifier itemForDistance(double distance) {
        if (distance < NEAR_DISTANCE) {
            return NEAR_ITEM;
        }
        if (distance < MIDDLE_DISTANCE) {
            return MIDDLE_ITEM;
        }
        return FAR_ITEM;
    }

    /**
     * Get the segments of the guide lines to render this tick.
     *
     * @return An unmodifiable list of segments, empty when the lines are hidden.
     */
    public List<Segment> getSegments() {
        return Collections.unmodifiableList(this.segments);
    }
}
