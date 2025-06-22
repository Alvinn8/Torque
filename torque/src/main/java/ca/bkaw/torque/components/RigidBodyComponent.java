package ca.bkaw.torque.components;

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

public class RigidBodyComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "rigid_body"),
        RigidBodyComponent::new
    );

    // All vectors are stored in world coordinates.
    // The position is at the center of mass.

    // Properties
    private final double mass; // unit: kilogram
    private final Matrix3d localInertiaTensorInverse; // unit: (kg m^2)^-1, local to the unrotated vehicle's coordinate system

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
        this.mass = vehicle.getType().mass();
        this.localInertiaTensorInverse = new Matrix3d();
        // The world is not serialized. Use the world of the entity.
        this.world = null;
        this.position = data.readVector3d("position", new Vector3d());
        this.velocity = data.readVector3d("velocity", new Vector3d());
        this.orientation = data.readQuaternionf("orientation", new Quaternionf());
        this.angularVelocity = data.readVector3d("angular_velocity", new Vector3d());
        this.netForce = new Vector3d();
        this.netTorque = new Vector3d();
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {
        data.writeVector3d("position", this.position);
        data.writeVector3d("velocity", this.velocity);
        data.writeQuaternionf("orientation", this.orientation);
        data.writeVector3d("angular_velocity", this.angularVelocity);
    }

    @Override
    public void tick(Vehicle vehicle) {
        final double deltaTime = 1 / 20.0; // one tick, unit: second

        // Apply linear motion.
        Vector3d acceleration = this.netForce.div(this.mass); // unit: meter/second^2
        this.velocity.add(acceleration.mul(deltaTime));
        this.position.add(new Vector3d(this.velocity).mul(deltaTime));
        this.netForce.zero();

        // Apply angular motion.
        // TODO
        this.netTorque.zero();
    }

    /**
     * Add a force acting on the rigid body.
     *
     * @param force The force vector in world coordinates. Unit: Newton.
     * @param point The point of application in world coordinates. Unit: meter.
     */
    public void addForce(Vector3dc force, Vector3dc point) {
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

    public void setWorld(@NotNull World world) {
        this.world = world;
    }

    public void setPosition(Vector3dc position) {
        this.position.set(position);
    }
}
