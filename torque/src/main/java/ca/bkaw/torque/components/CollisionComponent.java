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
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
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

    @Override
    public void tick(Vehicle vehicle) {
        vehicle.getComponent(RigidBodyComponent.class).ifPresent(rbc -> {
            this.obb = new OBB(
                new Vector3d(rbc.getPosition()).add(new Vector3d(0, 0.8, 0).rotate(new Quaterniond(rbc.getOrientation()))),
                new Vector3d(1, 0.8, 2.25),
                rbc.getOrientation()
            );
            // ((Quaternionf) rbc.getOrientation()).identity();

            if (this.boundingBoxDisplay == null) {
                this.boundingBoxDisplay = rbc.getWorld().spawnItemDisplay(rbc.getPosition());
                this.boundingBoxDisplay.setTeleportDuration(0);
                this.boundingBoxDisplay.setItem(vehicle.getTorque().getPlatform().createModelItem(new Identifier("minecraft", "glass")));
            }

            this.obb.visualize(this.boundingBoxDisplay);

            Vector3d desiredVelocity = new Vector3d(rbc.getVelocity());
            Vector3d desiredNetForce = new Vector3d(rbc.getNetForce());

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
                    // Vector3d absDirection = new Vector3d(direction).absolute();
                    // if (absDirection.x > absDirection.y && absDirection.x > absDirection.z) {
                    //     direction.set(direction.x, 0, 0);
                    // } else if (absDirection.y > absDirection.x && absDirection.y > absDirection.z) {
                    //     direction.set(0, direction.y, 0);
                    // } else {
                    //     direction.set(0, 0, direction.z);
                    // }

                    // direction.normalize().mul(desiredDeltaPosition.dot(direction));
                    Vector3d projection = direction.mul(desiredVelocity.dot(direction) / direction.lengthSquared());
                    desiredVelocity.sub(projection);

                    direction = new Vector3d(blockPos).sub(rbc.getPosition());
                    Vector3d projection2 = direction.mul(desiredNetForce.dot(direction) / direction.lengthSquared());
                    desiredNetForce.sub(projection2);
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

            // Calculate the force needed to convert the delta position to the desired delta position.
            Vector3d velocityDifference = desiredVelocity.sub(rbc.getVelocity());
            Vector3d forceDifference = desiredNetForce.sub(rbc.getNetForce());

            final double invDeltaTime = 20; // Unit: s^-1
            Vector3d force = forceDifference.add(velocityDifference.mul(vehicle.getType().mass() * invDeltaTime));
            force.mul(1.2);
            if (Math.random() < 0.05) {
                // System.out.println("velocity = " + Util.formatSi("m/s", rbc.getVelocity()));
                // System.out.println("netForce = " + Util.formatSi("N", rbc.getNetForce()));
                System.out.println("force = " + Util.formatSi("N", force));
            }
            rbc.addForce(force, rbc.getPosition());
        });
    }

}
