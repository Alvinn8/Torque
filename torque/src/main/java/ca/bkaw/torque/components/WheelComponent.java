package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.physics.VelocityConstraint;
import ca.bkaw.torque.platform.Input;
import ca.bkaw.torque.tags.WheelTags;
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

    private static final class WheelData {
        private final WheelTags.Wheel wheel;
        private float rotation; // unit: rad
        private float speed; // unit: rad/s
        private double steerAngle; // unit: rad
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
        Vector3d vehicleUp = new Vector3d(0, 1, 0).rotate(orientation);

        float steeringWheelAngle = vehicle.getComponent(SteeringWheelComponent.class)
            .map(SteeringWheelComponent::getAngle)
            .orElse(0.0f);

        // Ackermann steering geometry
        double turningRadius = this.getTurningRadius(steeringWheelAngle);
        if (Double.isFinite(turningRadius) && Math.abs(turningRadius) < 30) {
            Debug.highlightPositionSmall(rbc.getWorld(), new Vector3d(turningRadius, 0, this.backAxleOffset).rotate(orientation).add(vehiclePosition), "blue_wool");
        }

        // Shared across wheels since we assume the vehicle's weight is spread evenly
        // across them (no suspension/load-transfer model yet).
        Matrix3d inertiaTensorInverse = rbc.getInertiaTensorInverse(vehicle);
        double normalForcePerWheel = vehicle.getType().mass() * GravityComponent.GRAVITATIONAL_ACCELERATION / this.wheels.size();
        double frictionForceLimit = TIRE_FRICTION_COEFFICIENT * normalForcePerWheel; // unit: Newton

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

            Vector3d wheelRight = new Vector3d(vehicleUp).cross(wheelForward);

            Vector3d worldContactPatch = new Vector3d(wheel.wheel.contactPatch())
                .add(vehicle.getType().model().getPrimary().translation())
                .rotate(orientation)
                .add(vehiclePosition);

            // Apply driving force from engine to driven wheels, limited by available
            // traction. If the requested force exceeds what the tire can transmit,
            // the wheel spins instead (the excess is simply not applied).
            double longitudinalForce = 0;
            if (wheel.wheel.driven()) {
                double requestedForce = driveMagnitude / this.numberOfDrivenWheels;
                longitudinalForce = Math.max(-frictionForceLimit, Math.min(frictionForceLimit, requestedForce));
                Vector3d drivingForce = new Vector3d(wheelForward).mul(longitudinalForce);
                rbc.addForce(drivingForce, worldContactPatch);
                Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch, new Vector3d(drivingForce).div(1000), "red_wool");
            }

            // Update visual wheel rotation
            wheel.rotation += wheel.speed * (float) RigidBodyComponent.DELTA_TIME;

            // Cornering (lateral) constraint: resists sideways slip at the contact
            // patch, instead of a slip-angle spring that has to "catch up" after slip
            // has already happened. The available lateral force is whatever traction
            // the driving force above hasn't already used (a basic friction circle),
            // so a wheel that is spinning or braking hard has less grip left over for
            // cornering, and a wheel that demands more cornering force than the tire
            // can provide will still slip sideways - the clamp is what decides that,
            // not vehicle speed. The constraint is registered on the rigid body,
            // which solves all constraints (every wheel, and any collision contacts)
            // together in its post-tick phase, so a wheel's correction is negotiated
            // with everything else acting on the vehicle within the same tick.
            double remainingLateralLimit = Math.sqrt(Math.max(0,
                frictionForceLimit * frictionForceLimit - longitudinalForce * longitudinalForce));
            double maxLateralImpulse = remainingLateralLimit * RigidBodyComponent.DELTA_TIME;

            // Read back what the tire resolved to last tick, for warm starting and
            // for the debug visualization of the applied force.
            double previousImpulse = 0;
            if (wheel.lateralConstraint != null) {
                previousImpulse = wheel.lateralConstraint.getTotalImpulse();
                Debug.visualizeVectorAt(rbc.getWorld(), worldContactPatch,
                    new Vector3d(wheelRight).mul(previousImpulse / RigidBodyComponent.DELTA_TIME / 1000), "pink_wool");
            }

            VelocityConstraint lateralConstraint = new VelocityConstraint(
                worldContactPatch, vehiclePosition, wheelRight,
                1.0 / vehicle.getType().mass(), inertiaTensorInverse
            )
                .impulseBounds(-maxLateralImpulse, maxLateralImpulse)
                .warmStart(previousImpulse);
            rbc.addConstraint(lateralConstraint);
            wheel.lateralConstraint = lateralConstraint;
        }
    }

    /**
     * Get the turning radius that the vehicle follows for the given steering wheel
     * angle, according to the Ackermann steering geometry.
     * <p>
     * The turning circle's center is located at {@code (turningRadius, 0, backAxleOffset)}
     * in model coordinates, so a positive radius means the center is on the right
     * side of the vehicle (a right turn) and a negative radius means a left turn.
     *
     * @param steeringWheelAngle The steering wheel angle. Unit: radians.
     * @return The signed turning radius, or {@link Double#POSITIVE_INFINITY} when
     * driving straight. Unit: meters.
     */
    public double getTurningRadius(float steeringWheelAngle) {
        float averageSteeringAngle = steeringWheelAngle * STEERING_WHEEL_RATIO;
        if (Math.abs(averageSteeringAngle) < 1e-6) {
            return Double.POSITIVE_INFINITY;
        }
        return this.wheelbase * Math.tan(Math.PI / 2 - averageSteeringAngle);
    }

    /**
     * Get the z coordinate of the back axle in model coordinates.
     * <p>
     * In model coordinates +Z is backwards.
     *
     * @return The offset. Unit: meters.
     */
    public double getBackAxleOffset() {
        return this.backAxleOffset;
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
                break;
            }
        }

        return new PartTransform(rotation);
    }

}
