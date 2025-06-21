package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
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

    public RigidBodyComponent(Vehicle vehicle) {
        this.mass = 1500;
        this.localInertiaTensorInverse = new Matrix3d();
        this.world = null;
        this.position = new Vector3d();
        this.velocity = new Vector3d();
        this.orientation = new Quaternionf();
        this.angularVelocity = new Vector3d();
        this.netForce = new Vector3d();
        this.netTorque = new Vector3d();
    }

    public RigidBodyComponent(double mass, Matrix3d localInertiaTensor, World world, Vector3d position, Quaternionf orientation) {
        this.mass = mass;
        this.localInertiaTensorInverse = localInertiaTensor.invert();
        this.world = world;
        this.position = position;
        this.velocity = new Vector3d();
        this.orientation = orientation;
        this.angularVelocity = new Vector3d();
        this.netForce = new Vector3d();
        this.netTorque = new Vector3d();
    }

    @Override
    public void tick() {
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
    public void addForce(Vector3d force, Vector3d point) {
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

    public Quaternionfc getOrientation() {
        if (false) {
            this.orientation.rotateAxis(0.05f, 0, 1, 0);
        }
        return this.orientation;
    }
}
