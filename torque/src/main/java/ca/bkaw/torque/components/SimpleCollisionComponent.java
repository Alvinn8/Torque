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
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;

public class SimpleCollisionComponent implements VehicleComponent {
    public static final VehicleComponentType TYPE = VehicleComponentType.create(
        new Identifier("torque", "simple_collision"),
        SimpleCollisionComponent::new
    );

    private OBB obb;
    private @Nullable Vector3i highestCollisionBlock;

    /**
     * Whether the vehicle has ground contact.
     * <p>
     * Note that if the vehicle is rotated, the "ground" may not be the actual ground
     * but the surface at the vehicle's bottom.
     */
    private boolean onGround = false;
    /**
     * Whether the vehicle is colliding with any block in any direction.
     */
    private boolean isColliding = false;

    public SimpleCollisionComponent(Vehicle vehicle, DataInput dataInput) {}

    @Override
    public @NotNull VehicleComponentType getType() {
        return TYPE;
    }

    @Override
    public void save(Vehicle vehicle, DataOutput data) {}

    @Override
    public void tick(Vehicle vehicle) {}

    public void run(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            this.obb = new OBB(
                new Vector3d(rbc.getPosition()).add(new Vector3d(0, 0.8, 0).rotate(new Quaterniond(rbc.getOrientation()))),
                new Vector3d(1, 0.8, 2.25),
                rbc.getOrientation()
            );
            World world = rbc.getWorld();
            Debug.visualizeObb(world, this.obb, "glass");

            Vector3dc center = this.obb.getCenter();
            Vector3d velocity = rbc.getVelocity();

            // Local axes
            Quaterniond orientation = new Quaterniond(rbc.getOrientation());
            Vector3d right = new Vector3d(1, 0, 0).rotate(orientation);
            Vector3d up = new Vector3d(0, 1, 0).rotate(orientation);
            Vector3d backward = new Vector3d(0, 0, 1).rotate(orientation);

            // Project velocity onto local axes
            double velRight = velocity.dot(right);
            double velUp = velocity.dot(up);
            double velBackward = velocity.dot(backward);

            // Variables needed for stepping
            double originalVelRight = velRight;
            double originalVelBackward = velBackward;
            this.highestCollisionBlock = null;

            this.onGround = false;
            this.isColliding = false;

            // Variables needed for collision checks
            Vector3dc halfSize = this.obb.getHalfSize();
            Vector3d checkPosition = new Vector3d();

            // As an optimization, offsetUp loops backwards so that the highest up value is checked first.
            // That way, the highestCollisionBlock is set to the highest block that collides first,
            // and we can skip further checks since we know a collision will occur. Note that this only
            // works if the vehicle is upright (the up vector is roughly aligned with the world up vector).
            // This is the case most of the time, so assume it is true. If not true, the vehicle may step
            // incorrectly sometimes.

            // right/left collision
            if (Math.abs(velRight) > 1e-6) {
                double offsetRight = velRight * RigidBodyComponent.DELTA_TIME + (velRight > 0 ? halfSize.x() : -halfSize.x());

                rightLoop:
                for (double offsetUp = halfSize.y(); offsetUp >= -halfSize.y(); offsetUp -= 1) {
                    checkPosition.set(center)
                        .fma(offsetRight, right) // + offsetRight * right
                        .fma(offsetUp, up) // + offsetUp * up
                        .fma(-halfSize.z(), backward); // Start at the right position
                    for (double offsetBackward = -halfSize.z(); offsetBackward <= halfSize.z(); offsetBackward += 1) {
                        if (this.doCollisionBlockCheck(world, checkPosition)) {
                            velRight = 0;
                            this.isColliding = true;
                            break rightLoop;
                        }
                        checkPosition.add(backward); // checkPosition += backward
                    }
                }
            }

            // up/down collision
            if (Math.abs(velUp) > 1e-6) {
                double offsetUp = velUp * RigidBodyComponent.DELTA_TIME + (velUp > 0 ? halfSize.y() : -halfSize.y());

                for (double offsetRight = -halfSize.x(); offsetRight <= halfSize.x(); offsetRight += 1) {
                    checkPosition.set(center)
                        .fma(offsetUp, up) // + offsetUp * up
                        .fma(offsetRight, right) // + offsetRight * right
                        .fma(-halfSize.z(), backward); // Start at the right position
                    for (double offsetBackward = -halfSize.z(); offsetBackward <= halfSize.z(); offsetBackward += 1) {
                        if (this.doCollisionBlockCheck(world, checkPosition)) {
                            if (velUp < 0) {
                                // If the vehicle is moving dow into the ground.
                                this.onGround = true;
                            }
                            velUp = 0;
                            this.isColliding = true;
                            break;
                        }
                        checkPosition.add(backward); // checkPosition += * backward
                    }
                }
            }

            // forward/backward collision
            if (Math.abs(velBackward) > 1e-6) {
                double offsetBackward = velBackward * RigidBodyComponent.DELTA_TIME + (velBackward > 0 ? halfSize.z() : -halfSize.z());

                backwardLoop:
                for (double offsetUp = halfSize.y(); offsetUp >= -halfSize.y(); offsetUp -= 1) {
                    checkPosition.set(center)
                        .fma(offsetBackward, backward) // + offsetBackward * backward
                        .fma(offsetUp, up) // + offsetUp * up
                        .fma(-halfSize.x(), right); // Start at the right position
                    for (double offsetRight = -halfSize.x(); offsetRight <= halfSize.x(); offsetRight += 1) {
                        if (this.doCollisionBlockCheck(world, checkPosition)) {
                            velBackward = 0;
                            this.isColliding = true;
                            break backwardLoop;
                        }
                        checkPosition.add(right); // checkPosition += * right
                    }
                }
            }

            // Stepping
            int currentY = (int) Math.floor(new Vector3d(center).fma(-halfSize.y(), up).y() + 0);
            Debug.highlightFullBlock(world, new Vector3i((int) Math.floor(center.x()), currentY, (int) Math.floor(center.z())), "yellow_wool");
            if (this.highestCollisionBlock != null && this.highestCollisionBlock.y() == currentY) {
                // If the highest collision block is at the same height as the vehicle, we can step up
                rbc.setPosition(new Vector3d(rbc.getPosition()).add(up)); // Not the spirit :(
                velRight = originalVelRight;
                velBackward = originalVelBackward;
            }

            rbc.setVelocity(new Vector3d()
                .fma(velRight, right)
                .fma(velUp, up)
                .fma(velBackward, backward));
        });
    }

    private final Vector3i blockPos = new Vector3i(); // Performance optimization to avoid creating a new object each time

    private boolean doCollisionBlockCheck(World world, Vector3d position) {
        blockPos.set(position, RoundingMode.FLOOR);
        if (world.getBlock(blockPos).isCollidable(world, blockPos)) {
            if (this.highestCollisionBlock == null || blockPos.y > this.highestCollisionBlock.y) {
                this.highestCollisionBlock = new Vector3i(blockPos);
            }
            Debug.highlightFullBlock(world, blockPos, "red_wool");
            return true;
        }
        Debug.highlightBlockSmall(world, blockPos, "stone");
        return false;
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public boolean isColliding() {
        return this.isColliding;
    }
}
