package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.util.Util;
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

public class RigidBodyComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "rigid_body"),
        RigidBodyComponent::new
    );// one tick, unit: second
    public static final double DELTA_TIME = 1 / 20.0;

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

        // Apply linear motion.
        Vector3d acceleration = this.netForce.div(vehicle.getType().mass()); // unit: meter/second^2
        this.velocity.add(acceleration.mul(DELTA_TIME));
        this.position.add(this.velocity.mul(DELTA_TIME, new Vector3d()));
        this.netForce.zero();

        // Apply angular motion.
        Matrix3d worldInertiaTensorInverse = this.getInertiaTensorInverse(vehicle); // unit: (kg m^2)^-1
        Vector3d angularAcceleration = this.netTorque.mul(worldInertiaTensorInverse); // unit: radians/second^2
        this.angularVelocity.add(angularAcceleration.mul(DELTA_TIME));
        // Update orientation based on angular velocity.
        float angle = (float) (this.angularVelocity.length() * DELTA_TIME); // unit: radians
        if (angle > 1e-6) {
            Quaternionf deltaOrientation = new Quaternionf().rotateAxis(
                angle,
                (float) this.angularVelocity.x(), (float) this.angularVelocity.y(), (float) this.angularVelocity.z()
            );
            this.orientation.mul(deltaOrientation).normalize();
        }
        this.netTorque.zero();
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
        System.out.println("setting world to " + world);
        this.world = world;
    }

    public void setPosition(Vector3dc position) {
        this.position.set(position);
    }
}
