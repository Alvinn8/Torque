package ca.bkaw.torque.components;

import ca.bkaw.torque.physics.VelocityConstraint;
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
import org.joml.Vector3ic;

/**
 * Collision handling that registers non-penetration contact constraints on the
 * rigid body, to be solved together with all other constraints acting on the
 * vehicle (such as the wheels' lateral grip). This lets collision and tire forces
 * negotiate within the same tick - e.g. a wheel steering the vehicle into a wall
 * finds its available correction reduced by the wall's contact constraint - instead
 * of each independently correcting the other's result a tick late.
 */
public class ImpulseCollisionComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "impulse_collision"),
        ImpulseCollisionComponent::new
    );

    private OBB obb;

    public ImpulseCollisionComponent(Vehicle vehicle, DataInput dataInput) {

    }

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            this.obb = new OBB(
                new Vector3d(rbc.getPosition()).add(new Vector3d(0, 0.8, 0).rotate(new Quaterniond(rbc.getOrientation()))),
                new Vector3d(1, 0.8, 2.25),
                rbc.getOrientation()
            );
            Debug.visualizeObb(rbc.getWorld(), this.obb, "glass");

            World world = rbc.getWorld();
            double inverseMass = 1.0 / vehicle.getType().mass();
            Matrix3d inertiaTensorInverse = rbc.getInertiaTensorInverse(vehicle);

            for (Vector3ic blockPos : this.obb.getBlocksInsideApprox()) {
                if (!world.getBlock(blockPos).isCollidable(world, blockPos)) {
                    // The block is passable. For debug, show a stone block.
                    Debug.highlightBlockSmall(world, blockPos, "stone");
                    continue;
                }

                // Approximate the contact normal as the block face closest to the
                // direction from the block towards the vehicle's center of mass.
                Vector3d blockCenter = new Vector3d(blockPos).add(0.5, 0.5, 0.5);
                Vector3d direction = new Vector3d(rbc.getPosition()).sub(blockCenter);
                Vector3d absDirection = new Vector3d(direction).absolute();
                Vector3d normal;
                if (absDirection.x > absDirection.y && absDirection.x > absDirection.z) {
                    normal = new Vector3d(Math.signum(direction.x), 0, 0);
                } else if (absDirection.y > absDirection.x && absDirection.y > absDirection.z) {
                    normal = new Vector3d(0, Math.signum(direction.y), 0);
                } else {
                    normal = new Vector3d(0, 0, Math.signum(direction.z));
                }
                Vector3d contactPosition = blockCenter.add(normal.mul(0.5, new Vector3d()));

                // A contact may push the vehicle away from the surface but never pull
                // it in, so the impulse is bounded to [0, infinity). The target
                // relative velocity of zero means an inelastic collision (no bounce).
                //
                // No tangential (friction) constraints are registered for these
                // contacts yet: ground friction is modeled at the wheels, and chassis
                // friction against walls needs material data to make sense. Contacts
                // are also not warm started, since that requires matching this tick's
                // contacts to last tick's.
                VelocityConstraint contact = new VelocityConstraint(
                    contactPosition, rbc.getPosition(), normal,
                    inverseMass, inertiaTensorInverse
                ).impulseBounds(0, Double.MAX_VALUE);
                rbc.addConstraint(contact);

                Debug.highlightFullBlock(world, blockPos, "red_wool");
            }
        });
    }
}
