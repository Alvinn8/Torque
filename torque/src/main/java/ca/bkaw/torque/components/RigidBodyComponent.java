package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.vehicle.VehicleComponent;
import org.joml.Matrix3d;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class RigidBodyComponent implements VehicleComponent {
    // All vectors are stored in world coordinates.
    // The position is at the center of mass.

    // Properties
    private final double mass; // unit: kilogram
    private final Matrix3d initialInertiaTensorInverse; // unit: (kg m^2)^-1

    // Position and velocity
    private World world;
    private final Vector3d position; // unit: meter
    private final Vector3d velocity; // unit: meter/second
    private final Quaternionf orientation;
    private final Vector3d angularVelocity; // unit: radian/second

    // Accumulated each frame
    private final Vector3d netForce; // unit: Newton
    private final Vector3d netTorque; // unit: Newton-meter

    public RigidBodyComponent(double mass, Matrix3d initialInertiaTensor, World world, Vector3d position, Quaternionf orientation) {
        this.mass = mass;
        this.initialInertiaTensorInverse = initialInertiaTensor.invert();
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
        if (this.netForce.lengthSquared() > 0.01) {
            System.out.println("Net force: " + this.netForce.length() + " N");
        }

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

    public Vector3d getPosition() {
        return this.position;
    }

    public Quaternionf getOrientation() {
        return this.orientation;
    }
}
