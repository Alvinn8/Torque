package ca.bkaw.torque.components;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.ItemDisplay;
import ca.bkaw.torque.platform.World;
import ca.bkaw.torque.util.OBB;
import ca.bkaw.torque.util.Util;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleComponent;
import ca.bkaw.torque.vehicle.VehicleComponentType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix4f;
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
    private @Nullable ItemDisplay boundingBoxDisplay = null;
    private List<ItemDisplay> blockPositionDisplays = new ArrayList<>();

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

            if (this.boundingBoxDisplay == null) {
                this.boundingBoxDisplay = rbc.getWorld().spawnItemDisplay(rbc.getPosition());
                this.boundingBoxDisplay.setTeleportDuration(0);
                this.boundingBoxDisplay.setItem(vehicle.getTorque().getPlatform().createModelItem(new Identifier("minecraft", "glass")));
            }

            this.obb.visualize(this.boundingBoxDisplay);

            List<ContactPoint> contactPoints = this.getContactPoints(vehicle, rbc);

            if (!contactPoints.isEmpty()) {
                this.handleCollisions(vehicle, rbc, contactPoints);
            }
        });
    }

    private List<ContactPoint> getContactPoints(Vehicle vehicle, RigidBodyComponent rbc) {
        List<ContactPoint> contactPoints = new ArrayList<>();

        int index = 0;
        World world = rbc.getWorld();
        for (Vector3ic blockPos : this.obb.getBlocksInsideApprox()) {
            ItemDisplay display;
            if (index >= this.blockPositionDisplays.size()) {
                display = world.spawnItemDisplay(new Vector3d(blockPos).add(0.5, 0.5, 0.5));
                display.setTeleportDuration(0);
                this.blockPositionDisplays.add(display);
            } else {
                display = this.blockPositionDisplays.get(index);
            }
            index++;
            if (world.getBlock(blockPos).isCollidable(rbc.getWorld(), blockPos)) {
                display.setItem(vehicle.getTorque().getPlatform().createModelItem(new Identifier("minecraft", "red_wool")));
                display.setTransformation(new Matrix4f().translate(0, -20, 0).scale(1.05f, 1.05f, 1.05f));
                display.setPosition(new Vector3d(blockPos).add(0.5, 20.5, 0.5));

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

                Vector3d contactPosition = new Vector3d(blockPos).add(0.5, 0.5, 0.5).add(direction.mul(0.5, new Vector3d()));
                contactPoints.add(new ContactPoint(contactPosition, direction.negate(), new Vector3d()));
            } else {
                // The block is passable. For debug, show a stone block.
                display.setItem(vehicle.getTorque().getPlatform().createModelItem(new Identifier("minecraft", "stone")));
                display.setTransformation(new Matrix4f().scale(0.2f, 0.2f, 0.2f));
                display.setPosition(new Vector3d(blockPos).add(0.5, 0.5, 0.5));
            }
        }
        while (index < this.blockPositionDisplays.size()) {
            ItemDisplay display = this.blockPositionDisplays.removeLast();
            display.remove();
        }
        return contactPoints;
    }

    private void handleCollisions(Vehicle vehicle, RigidBodyComponent rbc, List<ContactPoint> contactPoints) {
        final double deltaTime = 1 / 20.0; // one tick, unit: seconds
        final boolean debug = false;

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
            if (debug) System.out.println("===================== Iteration " + iteration + " ====================");

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
                    if (debug) System.out.println("Contact Point " + contactPoint.position + " is moving away, relative velocity = " + Util.formatSi("m/s", relativeVelocity));
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
                if (debug) System.out.println("Contact Point " + contactPoint.position + " applying impulse " + Util.formatSi("Ns", impulseMagnitude) + " rel velocity = " + Util.formatSi("m/s", relativeVelocity));

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
        if (debug) System.out.println("iteration = " + iteration);

        // Now that collisions have been resolved at all contact points, we need to
        // apply all the impulses. This will ensure the actual velocity of the vehicle
        // matches the "next" velocities we have calculated above.
        //
        // The way we apply an impulse is to calculate the average force that would
        // be applied over the delta time, and then apply that force at the contact
        // point.

        for (ContactPoint contactPoint : contactPoints) {
            Vector3d averageForce = new Vector3d(contactPoint.impulse).div(deltaTime);
            rbc.addForce(averageForce, contactPoint.position);
        }
    }

}
