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
        if (rbc == null) {
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

        // Shared across wheels since we assume the vehicle's weight is spread evenly
        // across them (no suspension/load-transfer model yet).
        Matrix3d inertiaTensorInverse = rbc.getInertiaTensorInverse(vehicle);
        double normalForcePerWheel = vehicle.getType().mass() * GravityComponent.GRAVITATIONAL_ACCELERATION / this.wheels.size();
        double frictionForceLimit = TIRE_FRICTION_COEFFICIENT * normalForcePerWheel; // unit: Newton

        // A wheel's lateral constraint affects the vehicle's angular velocity, which in
        // turn changes what every other wheel's lateral constraint sees - e.g. one
        // wheel correcting its slip can spin the car just enough to induce slip in a
        // wheel on the other side. Solving each wheel's constraint independently
        // against a single shared snapshot of velocity/angular velocity would miss
        // that: it would compute the correction each wheel needs "if it were the only
        // one acting", and summing several such corrections together can overshoot.
        // So instead, gather all wheels' lateral constraints first and solve them
        // together against shared scratch state, the same Gauss-Seidel approach as
        // ImpulseCollisionComponent uses for multiple simultaneous collision contacts.
        record LateralConstraint(VelocityConstraint constraint, Vector3d contactPatch, double maxImpulse) {}
        List<LateralConstraint> lateralConstraints = new ArrayList<>(this.wheels.size());

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
            // not vehicle speed. It is solved below, together with every other wheel.
            double remainingLateralLimit = Math.sqrt(Math.max(0,
                frictionForceLimit * frictionForceLimit - longitudinalForce * longitudinalForce));
            double maxLateralImpulse = remainingLateralLimit * RigidBodyComponent.DELTA_TIME;

            VelocityConstraint lateralConstraint = new VelocityConstraint(
                worldContactPatch, vehiclePosition, wheelRight,
                1.0 / vehicle.getType().mass(), inertiaTensorInverse
            );
            lateralConstraints.add(new LateralConstraint(lateralConstraint, worldContactPatch, maxLateralImpulse));
        }

        // Solve all wheels' lateral constraints together against shared scratch
        // velocity/angular velocity, iterating so that the effect of correcting one
        // wheel's slip is visible to every other wheel's constraint within this same
        // tick, rather than only showing up a tick later once forces are integrated.
        Vector3d workingVelocity = new Vector3d(rbc.getVelocity());
        Vector3d workingAngularVelocity = new Vector3d(rbc.getAngularVelocity());
        for (int iteration = 0; iteration < 8; iteration++) {
            for (LateralConstraint lc : lateralConstraints) {
                double impulse = lc.constraint().solve(
                    workingVelocity, workingAngularVelocity, 0.0, -lc.maxImpulse(), lc.maxImpulse()
                );
                lc.constraint().applyImpulse(workingVelocity, workingAngularVelocity, impulse);
            }
        }

        // Express each wheel's total resolved impulse as an equivalent average force
        // over the tick, so it goes through the same addForce path as every other
        // force acting on the vehicle, and remains visible/limitable there.
        for (LateralConstraint lc : lateralConstraints) {
            Vector3d lateralForce = new Vector3d(lc.constraint().getDirection())
                .mul(lc.constraint().getTotalImpulse() / RigidBodyComponent.DELTA_TIME);
            rbc.addForce(lateralForce, lc.contactPatch());
            Debug.visualizeVectorAt(rbc.getWorld(), lc.contactPatch(), new Vector3d(lateralForce).div(1000), "pink_wool");
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
                break;
            }
        }

        return new PartTransform(rotation);
    }

}
