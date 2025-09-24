package ca.bkaw.torque.util;

import ca.bkaw.torque.Torque;
import ca.bkaw.torque.platform.Identifier;
import ca.bkaw.torque.platform.entity.ItemDisplay;
import ca.bkaw.torque.platform.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3ic;

import java.util.ArrayList;
import java.util.List;

public class Debug {
    private static @Nullable Debug instance;

    private final Torque torque;
    private final List<ItemDisplay> itemDisplays = new ArrayList<>();
    private int itemDisplayIndex = 0;

    public Debug(Torque torque) {
        this.torque = torque;
    }

    @Nullable
    public static Debug getInstance() {
        return instance;
    }

    public static void setInstance(@Nullable Debug instance) {
        Debug.instance = instance;
    }

    public static void print(String message) {
        if (instance == null) {
            return;
        }
        System.out.println("[DEBUG] " + message);
    }

    public ItemDisplay getItemDisplay(World world, Vector3dc position) {
        ItemDisplay display;
        if (this.itemDisplayIndex >= this.itemDisplays.size()) {
            display = world.spawnItemDisplay(position);
            display.setTeleportDuration(0);
            this.itemDisplays.add(display);
        } else {
            display = this.itemDisplays.get(this.itemDisplayIndex);
            display.setPosition(position);
            display.setTeleportDuration(0);
        }
        this.itemDisplayIndex++;

        return display;
    }

    public void tick() {
        // Remove unused item displays.
        while (this.itemDisplayIndex < this.itemDisplays.size()) {
            ItemDisplay display = this.itemDisplays.removeLast();
            display.remove();
        }
        this.itemDisplayIndex = 0;
    }

    public static void highlightFullBlock(World world, Vector3ic blockPos, String block) {
        if (instance == null) {
            return;
        }
        Vector3d position = new Vector3d(blockPos).add(0.5, 20.5, 0.5);
        ItemDisplay display = instance.getItemDisplay(world, position);
        display.setItem(instance.torque.getPlatform().createModelItem(new Identifier("minecraft", block)));
        display.setTransformation(new Matrix4f().translate(0, -20, 0).scale(1.05f, 1.05f, 1.05f));
    }

    public static void highlightBlockSmall(World world, Vector3ic blockPos, String block) {
        if (instance == null) {
            return;
        }
        Vector3d position = new Vector3d(blockPos).add(0.5, 0.5, 0.5);
        ItemDisplay display = instance.getItemDisplay(world, position);
        display.setItem(instance.torque.getPlatform().createModelItem(new Identifier("minecraft", block)));
        display.setTransformation(new Matrix4f().scale(0.2f, 0.2f, 0.2f));
    }

    public static void visualizeObb(World world, OBB obb, String block) {
        if (instance == null) {
            return;
        }
        Vector3d position = new Vector3d(obb.getCenter());
        ItemDisplay display = instance.getItemDisplay(world, position);
        display.setItem(instance.torque.getPlatform().createModelItem(new Identifier("minecraft", block)));
        Vector3dc halfSize = obb.getHalfSize();
        display.setTransformation(
            new Matrix4f()
                .rotate(obb.getRotation())
                .scale((float) (2 * halfSize.x()), (float) (2 * halfSize.y()), (float) (2 * halfSize.z()))
        );
    }

    public static void visualizeVectorAt(World world, Vector3dc position, Vector3dc vector, String block) {
        if (instance == null) {
            return;
        }
        Vector3d center = new Vector3d(position).add(new Vector3d(vector).mul(0.5));
        Quaternionf orientation = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), new Vector3f(vector));
        OBB obb = new OBB(
            center,
            new Vector3d(0.05, 0.05, vector.length() * 0.5),
            orientation
        );
        visualizeObb(world, obb, block);
    }
}
