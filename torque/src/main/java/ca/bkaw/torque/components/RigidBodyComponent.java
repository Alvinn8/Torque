package ca.bkaw.torque.components;

import ca.bkaw.torque.physics.VelocityConstraint;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class RigidBodyComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "rigid_body"),
        RigidBodyComponent::new
    );
    public static final double DELTA_TIME = 1 / 20.0; // one tick, unit: second

    /**
     * The number of Gauss-Seidel iterations used to solve the registered velocity
     * constraints each tick. Because constraint impulses are clamped, running out of
     * iterations before convergence degrades into slight residual slip/penetration
     * rather than instability.
     */
    public static final int SOLVER_ITERATIONS = 8;

    // All vectors are stored in world coordinates.
    // The position is at the center of mass.

    // Position and velocity
    private World world;
    private final Vector3d position; // unit: meter
    private final Vector3d velocity; // unit: meter/second
    private final Quaternionf orientation;
    private final Vector3d angularVelocity; // unit: radian/second

    // Accumulated each frame
    private final Vector3d netForce; // unit: Newton
    private final Vector3d netTorque; // unit: Newton-meter
    private final List<VelocityConstraint> constraints = new ArrayList<>();

    public RigidBodyComponent(Vehicle vehicle, DataInput data) {
        // The world is not serialized. Use the world of the entity.
        this.world = null;
        this.position = new Vector3d(data.readVector3f("position", new Vector3f()));
        this.velocity = new Vector3d(data.readVector3f("velocity", new Vector3f()));
        this.orientation = data.readQuaternionf("orientation", new Quaternionf());
        this.angularVelocity = new Vector3d(data.readVector3f("angular_velocity", new Vector3f()));
        this.netForce = new Vector3d();
        this.netTorque = new Vector3d();
    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {
        data.writeVector3f("position", new Vector3f(this.position));
        data.writeVector3f("velocity", new Vector3f(this.velocity));
        data.writeQuaternionf("orientation", this.orientation);
        data.writeVector3f("angular_velocity", new Vector3f(this.angularVelocity));
    }

    @Override
    public void tick(Vehicle vehicle) {
        // Forces and constraints are accumulated during the tick phase and integrated
        // in postTick, after all components have ticked, so that the component order
        // does not decide whether a force takes effect this tick or the next.
    }

    /**
     * Register a velocity constraint to be solved this tick.
     * <p>
     * All constraints registered during the tick phase are solved together in
     * {@link #postTick} with iterative sequential impulses, so constraints that
     * affect each other (wheels, collision contacts) negotiate within the tick
     * instead of fighting across ticks.
     *
     * @param constraint The constraint.
     */
    public void addConstraint(@NotNull VelocityConstraint constraint) {
        this.constraints.add(constraint);
    }

    @Override
    public void postTick(Vehicle vehicle) {
        // 1. Integrate external forces into velocity. This is the predicted velocity
        // the constraint solver corrects: constraints see the effect of this tick's
        // gravity, engine force, drag, etc. instead of last tick's.
        Vector3d acceleration = this.netForce.div(vehicle.getType().mass()); // unit: meter/second^2
        this.velocity.add(acceleration.mul(DELTA_TIME));
        this.netForce.zero();

        Matrix3d worldInertiaTensorInverse = this.getInertiaTensorInverse(vehicle); // unit: (kg m^2)^-1
        Vector3d angularAcceleration = this.netTorque.mul(worldInertiaTensorInverse); // unit: radians/second^2
        this.angularVelocity.add(angularAcceleration.mul(DELTA_TIME));
        this.netTorque.zero();

        // 2. Warm start: apply each constraint's impulse from last tick up front, so
        // a fixed number of iterations starts near the previous solution instead of
        // from zero.
        for (VelocityConstraint constraint : this.constraints) {
            double warmStartImpulse = constraint.getTotalImpulse();
            if (warmStartImpulse != 0) {
                constraint.applyImpulse(this.velocity, this.angularVelocity, warmStartImpulse);
            }
        }

        // 3. Solve all constraints together with sequential impulses (projected
        // Gauss-Seidel), so each constraint sees the corrections of the others.
        for (int i = 0; i < SOLVER_ITERATIONS; i++) {
            for (VelocityConstraint constraint : this.constraints) {
                constraint.solveIteration(this.velocity, this.angularVelocity);
            }
        }
        this.constraints.clear();

        // 4. Block collision handling that directly clamps velocity.
        vehicle.getComponent(SimpleCollisionComponent.class).ifPresent(simpleCollision -> simpleCollision.run(vehicle));

        // 5. Integrate velocity into position and orientation.
        this.position.add(this.velocity.mul(DELTA_TIME, new Vector3d()));

        float angle = (float) (this.angularVelocity.length() * DELTA_TIME); // unit: radians
        if (angle > 1e-6) {
            Quaternionf deltaOrientation = new Quaternionf().rotateAxis(
                angle,
                (float) this.angularVelocity.x(), (float) this.angularVelocity.y(), (float) this.angularVelocity.z()
            );
            this.orientation.mul(deltaOrientation).normalize();
        }

        // Dampen angular velocity to prevent jitter.
        if (this.angularVelocity.lengthSquared() < 1e-2) {
            this.angularVelocity.zero();
        }
    }

    /**
     * Get the inertia tensor inverse in world coordinates.
     * 
     * @param vehicle The vehicle that this rigid body component belongs to.
     * @return The inertia tensor inverse in world coordinates.
     */
    public Matrix3d getInertiaTensorInverse(Vehicle vehicle) {
        // TODO cache per tick?
        // Get the inertia tensor inverse in world coordinates.
        Matrix3d localInertiaTensorInverse = vehicle.getType().localInertiaTensorInverse();
        Matrix3d rotationMatrix = this.orientation.get(new Matrix3d());
        return new Matrix3d(rotationMatrix)
            .mul(localInertiaTensorInverse)
            .mul(rotationMatrix.transpose()); // unit: (kg m^2)^-1, world coordinates
    }

    /**
     * Add a force acting on the rigid body.
     *
     * @param force The force vector in world coordinates. unit: Newton.
     * @param point The point of application in world coordinates. unit: meter.
     */
    public void addForce(Vector3dc force, Vector3dc point) {
        if (!force.isFinite()) {
            throw new IllegalArgumentException("Force must be a finite vector.");
        }
        this.netForce.add(force);

        // The point is provided in world coordinates, so we need to convert it to local
        // coordinates for the cross product in the torque calculation.
        Vector3d localPoint = new Vector3d(point).sub(this.position); // unit: meter
        Vector3d torque = localPoint.cross(force); // unit: Newton-meter
        this.netTorque.add(torque);
    }

    public World getWorld() {
        return this.world;
    }

    public Vector3dc getPosition() {
        return this.position;
    }

    public Vector3d getVelocity() {
        return this.velocity;
    }

    public Quaternionfc getOrientation() {
        if (false) {
            this.orientation.rotateAxis(0.05f, 0, 1, 0);
        }
        return this.orientation;
    }

    /**
     * Get the current accumulated net force.
     *
     * @return The net force. Unit: Newton.
     */
    public Vector3d getNetForce() {
        return this.netForce;
    }

    public Vector3d getNetTorque() {
        return this.netTorque;
    }

    public Vector3d getAngularVelocity() {
        return this.angularVelocity;
    }

    public void setWorld(@NotNull World world) {
        this.world = world;
    }

    public void setPosition(Vector3dc position) {
        this.position.set(position);
    }

    public void setOrientation(Quaternionfc orientation) {
        this.orientation.set(orientation);
    }

    public void setVelocity(Vector3d nextVelocity) {
        this.velocity.set(nextVelocity);
    }

    public void setAngularVelocity(Vector3d angularVelocity) {
        this.angularVelocity.set(angularVelocity);
    }
}
