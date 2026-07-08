package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.physics.VelocityConstraint;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.tags.WheelTags;
import ca.bkaw.torque.terrain.TerrainSampler;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.vehicle.PartTransformationProvider;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Example component that controls wheel rotation based on vehicle movement.
 */
public class WheelComponent implements VehicleComponent, PartTransformationProvider {
    public static final VehicleComponentType TYPE = VehicleComponentType.builder(
        new Identifier("torque", "wheel")
    ).create(WheelComponent::new);

    /**
     * The ratio between how much the wheels turn for each degree of the steering wheel.
     */
    public static final float STEERING_WHEEL_RATIO = 1f / 10f;

    /**
     * Approximate coefficient of friction between tires and the ground, shared as one
     * traction budget between longitudinal (driving) and lateral (cornering) force.
     */
    private static final double TIRE_FRICTION_COEFFICIENT = 1.0;

    /**
     * The kernel radius of the terrain smoothing that wheels ride on. This decides
     * what terrain is drivable: a single 1-block step smooths into a ramp of average
     * grade about {@code atan(1/(2R))}, to be compared against the friction angle
     * {@code atan(TIRE_FRICTION_COEFFICIENT)}. See {@link TerrainSampler}.
     */
    private static final double TERRAIN_KERNEL_RADIUS = 1.0; // unit: meter

    /**
     * The undamped natural frequency of the suspension. Lower is softer: the spring
     * stiffness is chosen as {@code k = massPerWheel * (2 pi f)^2}, and the ground
     * offset at which the spring force reaches zero (full natural droop) is
     * {@code g / (2 pi f)^2}, about 0.25 m at 1 Hz.
     */
    private static final double SUSPENSION_FREQUENCY = 1.0; // unit: Hz

    /**
     * Damping ratio of the suspension. 1 = critically damped.
     */
    private static final double SUSPENSION_DAMPING_RATIO = 0.5; // unitless

    /**
     * How far the ground may drop below the wheel's rest position before the wheel
     * is considered airborne (and how far the wheel visually extends). Unit: meter.
     */
    private static final double SUSPENSION_MAX_DROOP = 0.35;

    /**
     * Cap on the suspension normal force, in multiples of the static per-wheel load,
     * so a deep compression cannot produce an explosive impulse in one tick.
     */
    private static final double SUSPENSION_MAX_FORCE_FACTOR = 4.0;

    /**
     * How much higher than the wheel's rest position the terrain scan window
     * reaches. Surfaces within the window count as terrain the wheel can ride on
     * (a 1-block step ahead must fit, or it could never be climbed); anything
     * taller is clamped and stays an obstacle for the chassis collision.
     * Unit: meter.
     */
    private static final double TERRAIN_RELIEF_ABOVE = 1.2;

    /**
     * How far below the wheel's rest position the terrain scan window reaches.
     * Ground further down than this is out of reach: the wheel is airborne.
     * Unit: meter.
     */
    private static final double TERRAIN_RELIEF_BELOW = SUSPENSION_MAX_DROOP + 2.0;

    private static final class WheelData {
        private final WheelTags.Wheel wheel;
        private float rotation; // unit: rad
        private float speed; // unit: rad/s
        private double steerAngle; // unit: rad
        /**
         * Visual suspension offset along the body's up axis: how far the wheel
         * renders above (+) or below (-) its modeled position. Unit: meter.
         */
        private float visualOffset;
        /**
         * The lateral constraint registered this tick. After the rigid body has
         * solved, its total impulse is what the tire actually applied; it is read
         * back next tick for warm starting and debug visualization.
         */
        private @Nullable VelocityConstraint lateralConstraint;

        public WheelData(WheelTags.Wheel wheel, float rotation, float speed) {
            this.wheel = wheel;
            this.rotation = rotation;
            this.speed = speed;
        }
    }

    private final List<WheelData> wheels = new ArrayList<>();
    private final double wheelbase;
    private final double backAxleOffset;
    private final int numberOfDrivenWheels;

    public WheelComponent(Vehicle vehicle, JsonObject config, DataInput dataInput) {
        vehicle.getType().model().getTagData(WheelTags.class)
            .ifPresent(wheels -> wheels.forEach(wheel ->
                this.wheels.add(new WheelData(wheel, 0, 0))
            ));
        double minZ = Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        int numberOfDrivenWheels = 0;
        for (WheelData wheel : this.wheels) {
            double z = wheel.wheel.contactPatch().z;
            if (z < minZ) minZ = z;
            if (z > maxZ) maxZ = z;
            if (wheel.wheel.driven()) {
                numberOfDrivenWheels++;
            }
        }
        this.wheelbase = maxZ - minZ;
        this.backAxleOffset = maxZ; // In model coordinates +Z is backwards
        this.numberOfDrivenWheels = numberOfDrivenWheels;
        Debug.print("wheelbase = " + this.wheelbase + ", backAxleOffset = " + this.backAxleOffset + ", numberOfDrivenWheels = " + this.numberOfDrivenWheels);
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        RigidBodyComponent rbc = vehicle.getComponent(RigidBodyComponent.class).orElse(null);
        if (rbc == null || this.wheels.isEmpty()) {
            return;
        }
        Vector3dc vehiclePosition = rbc.getPosition();
        Quaterniondc orientation = new Quaterniond(rbc.getOrientation());

        Input driverInput = vehicle.getComponent(SeatsComponent.class)
            .map(SeatsComponent::getDriverInput)
            .orElse(null);

        double driveMagnitude = 0;
        if (driverInput != null) {
            if (driverInput.forward) {
                driveMagnitude = 10_000.0;
            } else if (driverInput.backward) {
                driveMagnitude = -5_000.0;
            }
        }

        Vector3d vehicleForward = new Vector3d(0, 0, -1).rotate(orientation);

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);

        // Ackermann steering geometry
        float averageSteeringAngle = steeringWheelAngle * STEERING_WHEEL_RATIO;
        double turningRadius;
        if (Math.abs(averageSteeringAngle) < 1e-6) {
            turningRadius = Double.POSITIVE_INFINITY;
        } else {
            turningRadius = this.wheelbase * Math.tan(Math.PI / 2 - averageSteeringAngle);
        }
        if (Double.isFinite(turningRadius) && Math.abs(turningRadius) < 30) {
            Debug.highlightPositionSmall(rbc.getWorld(), new Vector3d(turningRadius, 0, this.backAxleOffset).rotate(orientation).add(vehiclePosition), "blue_wool");
        }

        Matrix3d inertiaTensorInverse = rbc.getInertiaTensorInverse(vehicle);

        // Suspension spring constants derived from the natural frequency, assuming
        // the vehicle's weight is spread evenly across the wheels at rest.
        double massPerWheel = vehicle.getType().mass() / this.wheels.size();
        double staticLoad = massPerWheel * GravityComponent.GRAVITATIONAL_ACCELERATION; // unit: Newton
        double omega = 2 * Math.PI * SUSPENSION_FREQUENCY; // unit: rad/s
        double springStiffness = massPerWheel * omega * omega; // unit: N/m
        double dampingCoefficient = 2 * SUSPENSION_DAMPING_RATIO * massPerWheel * omega; // unit: N s/m

        // Wheels do not ride on the raw block geometry (whose one-meter steps no
        // realistic wheel could climb) but on a smoothed terrain surface sampled
        // from it. The physics stays exact; only the geometry is reinterpreted.
        // See TerrainSampler.
        TerrainSampler terrain = new TerrainSampler(rbc.getWorld()::getGroundHeight, TERRAIN_KERNEL_RADIUS);

        for (WheelData wheel : this.wheels) {
            // Calculate direction vectors and the contact patch position.
            Vector3dc wheelForward = vehicleForward;
            if (wheel.wheel.steerable()) {
                // Ackermann steering geometry
                // Calculate the angle from wheel to the rotation center
                Vector3d cp = wheel.wheel.contactPatch();
                // delta y = distance between wheel and back axle
                // delta x = distance between wheel and turning circle
                // tan(steerAngle) = delta y / delta x
                // => steerAngle = arctan(delta y / delta x)
                wheel.steerAngle = Math.atan((this.backAxleOffset - cp.z) / (turningRadius - cp.x));
                wheelForward = new Vector3d(vehicleForward).rotateY(-wheel.steerAngle);
            }

            // Update visual wheel rotation.
            wheel.rotation += wheel.speed * (float) RigidBodyComponent.DELTA_TIME;

            // The wheel's contact position at rest (suspension neither compressed
            // nor drooped), moving and pitching with the body.
            Vector3d restContact = new Vector3d(wheel.wheel.contactPatch())
                .add(vehicle.getType().model().getPrimary().translation())
                .rotate(orientation)
                .add(vehiclePosition);

            // Sample the smoothed terrain under the wheel.
            TerrainSampler.GroundSample ground = terrain.sample(
                restContact.x, restContact.z,
                restContact.y + TERRAIN_RELIEF_ABOVE,
                restContact.y - TERRAIN_RELIEF_BELOW
            );

            if (Debug.getInstance() != null) {
                this.debugVisualizeSurface(rbc.getWorld(), terrain, restContact);
            }

            // How far the ground is above (+, compressing) or below (-, drooping)
            // the wheel's rest position.
            double groundOffset = ground != null ? ground.height() - restContact.y : Double.NEGATIVE_INFINITY;
            if (ground == null || groundOffset < -SUSPENSION_MAX_DROOP) {
                // Airborne: no ground within reach of the suspension. No normal
                // force, and therefore no traction for driving or cornering.
                wheel.visualOffset = (float) -SUSPENSION_MAX_DROOP;
                wheel.lateralConstraint = null;
                continue;
            }
            wheel.visualOffset = (float) Math.min(groundOffset, TERRAIN_RELIEF_ABOVE);

            Vector3d contactPoint = new Vector3d(restContact.x, ground.height(), restContact.z);
            Vector3dc normal = ground.normal();

            // Velocity of the wheel's contact point, including the body's rotation.
            Vector3d contactVelocity = new Vector3d(rbc.getAngularVelocity())
                .cross(new Vector3d(contactPoint).sub(vehiclePosition))
                .add(rbc.getVelocity());

            // Suspension normal force: a spring-damper around the static load, so a
            // wheel at rest position carries exactly its share of the weight. The
            // spring can push but never pull, and is capped so deep compressions do
            // not explode. Applied along the smoothed surface normal - on a slope
            // the normal tilts backward, which is what makes climbing cost speed.
            // (Compression and its rate are measured vertically; accurate for the
            // mostly-upright vehicle poses that have ground contact at all.)
            double springForce = staticLoad + springStiffness * groundOffset;
            double dampingForce = -dampingCoefficient * contactVelocity.y;
            double normalForce = Math.max(0, Math.min(
                springForce + dampingForce,
                SUSPENSION_MAX_FORCE_FACTOR * staticLoad
            ));
            rbc.addForce(new Vector3d(normal).mul(normalForce), contactPoint);
            Debug.visualizeVectorAt(rbc.getWorld(), contactPoint, new Vector3d(normal).mul(normalForce / 1000), "lime_wool");

            // Tire force directions lie in the contact plane: project the wheel's
            // forward direction onto the smoothed surface.
            Vector3d forwardTangent = new Vector3d(wheelForward)
                .sub(new Vector3d(normal).mul(wheelForward.dot(normal)));
            if (forwardTangent.lengthSquared() < 1e-12) {
                // Degenerate (surface normal parallel to wheel forward).
                wheel.lateralConstraint = null;
                continue;
            }
            forwardTangent.normalize();
            Vector3d rightTangent = new Vector3d(normal).cross(forwardTangent).negate();

            // Update visual wheel spin from the rolling speed.
            wheel.speed = (float) (contactVelocity.dot(forwardTangent) / wheel.wheel.radius());

            // The tire's traction budget this tick comes from the actual suspension
            // load on this wheel - an unloaded wheel has no grip, a loaded one has
            // more. Shared between longitudinal (drive) and lateral (cornering)
            // force: a basic friction circle.
            double frictionForceLimit = TIRE_FRICTION_COEFFICIENT * normalForce; // unit: Newton

            // Apply driving force from engine to driven wheels, limited by available
            // traction. If the requested force exceeds what the tire can transmit,
            // the wheel spins instead (the excess is simply not applied).
            double longitudinalForce = 0;
            if (wheel.wheel.driven()) {
                double requestedForce = driveMagnitude / this.numberOfDrivenWheels;
                longitudinalForce = Math.max(-frictionForceLimit, Math.min(frictionForceLimit, requestedForce));
                Vector3d drivingForce = new Vector3d(forwardTangent).mul(longitudinalForce);
                rbc.addForce(drivingForce, contactPoint);
                Debug.visualizeVectorAt(rbc.getWorld(), contactPoint, new Vector3d(drivingForce).div(1000), "red_wool");
            }

            // Cornering (lateral) constraint: resists sideways slip at the contact
            // point, instead of a slip-angle spring that has to "catch up" after slip
            // has already happened. The available lateral force is whatever traction
            // the driving force above hasn't already used, and a wheel that demands
            // more cornering force than the tire can provide will still slip
            // sideways - the clamp is what decides that, not vehicle speed. The
            // constraint is registered on the rigid body, which solves all
            // constraints (every wheel, and any collision contacts) together in its
            // post-tick phase, so a wheel's correction is negotiated with everything
            // else acting on the vehicle within the same tick.
            double remainingLateralLimit = Math.sqrt(Math.max(0,
                frictionForceLimit * frictionForceLimit - longitudinalForce * longitudinalForce));
            double maxLateralImpulse = remainingLateralLimit * RigidBodyComponent.DELTA_TIME;

            // Read back what the tire resolved to last tick, for warm starting and
            // for the debug visualization of the applied force.
            double previousImpulse = 0;
            if (wheel.lateralConstraint != null) {
                previousImpulse = wheel.lateralConstraint.getTotalImpulse();
                Debug.visualizeVectorAt(rbc.getWorld(), contactPoint,
                    new Vector3d(rightTangent).mul(previousImpulse / RigidBodyComponent.DELTA_TIME / 1000), "pink_wool");
            }

            VelocityConstraint lateralConstraint = new VelocityConstraint(
                contactPoint, vehiclePosition, rightTangent,
                1.0 / vehicle.getType().mass(), inertiaTensorInverse
            )
                .impulseBounds(-maxLateralImpulse, maxLateralImpulse)
                .warmStart(previousImpulse);
            rbc.addConstraint(lateralConstraint);
            wheel.lateralConstraint = lateralConstraint;
        }
    }

    /**
     * Render a grid of small planes showing the smoothed terrain surface around a
     * wheel, so the surface the wheel rides on (and its normals) can be inspected
     * in game. Only called while debug is enabled, since the grid costs a full
     * terrain sample per point.
     */
    private void debugVisualizeSurface(World world, TerrainSampler terrain, Vector3dc restContact) {
        final double spacing = 0.4;
        final int gridExtent = 2; // 5x5 planes per wheel
        for (int gridX = -gridExtent; gridX <= gridExtent; gridX++) {
            for (int gridZ = -gridExtent; gridZ <= gridExtent; gridZ++) {
                double x = restContact.x() + gridX * spacing;
                double z = restContact.z() + gridZ * spacing;
                TerrainSampler.GroundSample sample = terrain.sample(
                    x, z,
                    restContact.y() + TERRAIN_RELIEF_ABOVE,
                    restContact.y() - TERRAIN_RELIEF_BELOW
                );
                if (sample == null) {
                    continue;
                }
                Debug.visualizePlane(
                    world,
                    new Vector3d(x, sample.height(), z), sample.normal(),
                    spacing * 0.9, "light_blue_stained_glass"
                );
            }
        }
    }

    @Override
    public PartTransform getPartTransform(@NotNull String partName, @Nullable Object partData, @NotNull Vehicle vehicle) {
        if (!(partData instanceof WheelTags.Wheel wheel)) {
            return null;
        }

        Quaternionf rotation = new Quaternionf();

        for (WheelData wheelData : this.wheels) {
            if (wheelData.wheel == wheel) {
                if (wheel.steerable()) {
                    rotation.rotateY((float) -wheelData.steerAngle);
                }
                rotation.rotateX(-wheelData.rotation);
                // Show the suspension travel by moving the wheel along the body's
                // up axis to where the smoothed ground actually is.
                return new PartTransform(rotation, new Vector3f(0, wheelData.visualOffset, 0), false, null);
            }
        }

        return new PartTransform(rotation);
    }

}
