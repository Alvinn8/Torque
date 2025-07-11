package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.util.Debug;
import ca.bkaw.torque.util.OBB;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;

public class CollisionComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = new VehicleComponentType(
        new Identifier("torque", "collision"),
        CollisionComponent::new
    );

    private OBB obb;

    public CollisionComponent(Vehicle vehicle, DataInput dataInput) {

    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    private record ContactPoint(
        Vector3dc position,
        Vector3dc normal,
        Vector3d impulse
    ) {}

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            this.obb = new OBB(
                new Vector3d(rbc.getPosition()).add(new Vector3d(0, 0.8, 0).rotate(new Quaterniond(rbc.getOrientation()))),
                new Vector3d(1, 0.8, 2.25),
                rbc.getOrientation()
            );
            Debug.visualizeObb(rbc.getWorld(), this.obb, "glass");

            List<ContactPoint> contactPoints = this.getContactPoints(rbc);

            if (!contactPoints.isEmpty()) {
                this.handleCollisions(vehicle, rbc, contactPoints);
            }
        });
    }

    private List<ContactPoint> getContactPoints(RigidBodyComponent rbc) {
        List<ContactPoint> contactPoints = new ArrayList<>();

        World world = rbc.getWorld();
        for (Vector3ic blockPos : this.obb.getBlocksInsideApprox()) {
            if (world.getBlock(blockPos).isCollidable(rbc.getWorld(), blockPos)) {
                // Stop the delta position in this direction.
                Vector3d direction = new Vector3d(blockPos).sub(rbc.getPosition());
                // Find the closest axis-aligned direction.
                Vector3d absDirection = new Vector3d(direction).absolute();
                if (absDirection.x > absDirection.y && absDirection.x > absDirection.z) {
                    direction.set(direction.x, 0, 0);
                } else if (absDirection.y > absDirection.x && absDirection.y > absDirection.z) {
                    direction.set(0, direction.y, 0);
                } else {
                    direction.set(0, 0, direction.z);
                }
                direction.normalize();

                Vector3d normal = direction.negate();
                normal = new Vector3d(0, 1, 0);
                Vector3d contactPosition = new Vector3d(blockPos).add(0.5, 0.5, 0.5).add(normal.mul(0.5, new Vector3d()));
                contactPoints.add(new ContactPoint(contactPosition, normal, new Vector3d()));

                Debug.highlightFullBlock(world, blockPos, "red_wool");
                // Debug.visualizeVectorAt(world, contactPosition, normal, "pink_wool");
            } else {
                // The block is passable. For debug, show a stone block.
                Debug.highlightBlockSmall(world, blockPos, "stone");
            }
        }
        return contactPoints;
    }

    private void handleCollisions(Vehicle vehicle, RigidBodyComponent rbc, List<ContactPoint> contactPoints) {
        final double deltaTime = 1 / 20.0; // one tick, unit: seconds

        // Apply all external forces to see the acceleration without collisions.
        Vector3d predictedAcceleration = new Vector3d(rbc.getNetForce()).div(vehicle.getType().mass());

        Matrix3d inertiaTensorInverse = rbc.getInertiaTensorInverse(vehicle);
        Vector3d predictedAngularAcceleration = new Vector3d(rbc.getNetTorque()).mul(inertiaTensorInverse);

        // The velocity that would happen if there were no collisions.
        Vector3d predictedVelocity = new Vector3d(rbc.getVelocity()).add(predictedAcceleration.mul(deltaTime));
        Vector3d predictedAngularVelocity = new Vector3d(rbc.getAngularVelocity()).add(predictedAngularAcceleration.mul(deltaTime));

        // The velocity that will happen after applying the collisions.
        Vector3d nextVelocity = new Vector3d(predictedVelocity);
        Vector3d nextAngularVelocity = new Vector3d(predictedAngularVelocity);

        int iteration = 0;
        while (iteration < 10) {
            iteration++;
            boolean changed = false;
            // Debug.print("===================== Iteration " + iteration + " ====================");

            for (ContactPoint contactPoint : contactPoints) {
                // The vector from the vehicle's center of mass to the contact point.
                Vector3d r = new Vector3d(contactPoint.position).sub(rbc.getPosition());

                // The relative velocity at the contact point. Takes into account both the
                // linear velocity and the angular velocity.
                // v_rel = (v + omega × r) ⋅ n
                double relativeVelocity = new Vector3d(nextVelocity).add(
                    nextAngularVelocity.cross(r, new Vector3d())
                ).dot(contactPoint.normal); // Unit: m/s

                if (relativeVelocity > -0.01) {
                    // The vehicle is moving away from the surface, no impulse needed.
                    // Debug.print("Contact Point " + contactPoint.position + " is moving away, relative velocity = " + Util.formatSi("m/s", relativeVelocity));
                    continue;
                }

                double effectiveMass = 1.0 / (
                    1.0 / vehicle.getType().mass() +
                        contactPoint.normal.dot(
                            (new Vector3d(r).cross(contactPoint.normal) // r × n
                                .mul(inertiaTensorInverse)
                            ).cross(r, new Vector3d())
                        )
                ); // Unit: kg

                final double restitutionCoefficient = 0; // Unitless, 0 = inelastic, 1 = elastic
                double impulseMagnitude = -(1 + restitutionCoefficient) * relativeVelocity * effectiveMass;
                if (impulseMagnitude < 1e-3) {
                    // The impulse is negative. The contact point is trying to pull the vehicle.
                    // This is not allowed. Or it is very small, so we ignore it.
                    continue;
                }
                changed = true;
                // Debug.print("Contact Point " + contactPoint.position + " applying impulse " + Util.formatSi("Ns", impulseMagnitude) + " rel velocity = " + Util.formatSi("m/s", relativeVelocity));

                Vector3d impulse = new Vector3d(contactPoint.normal).mul(impulseMagnitude);

                // Keep track of the total impulse applied at by contact point.
                contactPoint.impulse.add(impulse);

                // Apply the impulse to the next velocity.
                // deltaV = impulse / mass
                Vector3d deltaVelocity = impulse.div(vehicle.getType().mass(), new Vector3d());
                nextVelocity.add(deltaVelocity);
                // deltaOmega = I^-1 * (r × impulse)
                Vector3d deltaAngularVelocity = new Vector3d(r).cross(impulse).mul(inertiaTensorInverse);
                nextAngularVelocity.add(deltaAngularVelocity);

                // Now that the next velocity and angular velocity have been updated,
                // this will affect the other contact points which may react by
                // applying other impulses. We repeat for a few iterations until
                // all contact points have been resolved so that the vehicle does
                // not clip through the surface at any contact point.
            }

            // Early exit if no impulses were applied this iteration.
            if (!changed) {
                break;
            }
        }
        // Debug.print("iteration = " + iteration);
        if (iteration >= 10) {
            Debug.print("CollisionComponent: Too many iterations, vehicle may be stuck in a collision.");
        }

        // Ensure no energy is added to the system.
        // This is important to prevent the vehicle from gaining energy from collisions.
        Matrix3d inertiaTensor = new Matrix3d(inertiaTensorInverse).invert();

        double currentLinearKineticEnergy = 0.5 * vehicle.getType().mass() * rbc.getVelocity().lengthSquared(); // Unit: Joules
        double currentAngularKineticEnergy = 0.5 * rbc.getAngularVelocity().dot(inertiaTensor.transform(rbc.getAngularVelocity(), new Vector3d())); // Unit: Joules
        double currentTotalKineticEnergy = currentLinearKineticEnergy + currentAngularKineticEnergy;

        double nextLinearKineticEnergy = 0.5 * vehicle.getType().mass() * nextVelocity.lengthSquared(); // Unit: Joules
        double nextAngularKineticEnergy = 0.5 * nextAngularVelocity.dot(inertiaTensor.transform(nextAngularVelocity, new Vector3d())); // Unit: Joules
        double nextTotalKineticEnergy = nextLinearKineticEnergy + nextAngularKineticEnergy;

        double scale = 1.0;
        if (nextTotalKineticEnergy > currentTotalKineticEnergy) {
            // Energy was added to the system. Scale down the next velocities.
            scale = Math.sqrt(currentTotalKineticEnergy / nextTotalKineticEnergy);
            scale = scale * 0.9; // Penalty for trying to create energy.
            nextVelocity.mul(scale);
            nextAngularVelocity.mul(scale);
        }

        // Now that collisions have been resolved at all contact points, we need to
        // apply all the impulses. This will ensure the actual velocity of the vehicle
        // matches the "next" velocities we have calculated above.
        //
        // The way we apply an impulse is to calculate the average force that would
        // be applied over the delta time, and then apply that force at the contact
        // point.

        rbc.setVelocity(nextVelocity);
        rbc.setAngularVelocity(nextAngularVelocity);

        // for (ContactPoint contactPoint : contactPoints) {
        //     Vector3d averageForce = new Vector3d(contactPoint.impulse).div(deltaTime).mul(scale);
        //     rbc.addForce(averageForce, contactPoint.position);
        //     Debug.visualizeVectorAt(rbc.getWorld(), contactPoint.position, new Vector3d(averageForce).div(1000), "pink_wool");
        // }
    }

}
