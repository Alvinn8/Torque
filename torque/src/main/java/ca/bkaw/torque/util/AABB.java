package ca.bkaw.torque.util;

/**
 * An axis-aligned bounding box (AABB).
 */
public class AABB {
    private final double minX;
    private final double minY;
    private final double minZ;
    private final double maxX;
    private final double maxY;
    private final double maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean contains(double x, double y, double z) {
        return x >= this.minX && x <= this.maxX &&
               y >= this.minY && y <= this.maxY &&
               z >= this.minZ && z <= this.maxZ;
    }

    public boolean contains(AABB other) {
        return this.minX <= other.minX && this.maxX >= other.maxX &&
               this.minY <= other.minY && this.maxY >= other.maxY &&
               this.minZ <= other.minZ && this.maxZ >= other.maxZ;
    }
}
