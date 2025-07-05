package ca.bkaw.torque.util;

import ca.bkaw.torque.platform.ItemDisplay;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionfc;
import org.joml.RoundingMode;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;

import java.util.HashSet;
import java.util.Set;

/**
 * An oriented bounding box (OBB).
 * <p>
 * An OBB is a bounding box not necessarily aligned with the axes.
 */
public class OBB {
    private final Vector3dc center;
    private final Vector3dc halfSize;
    private final Quaternionfc rotation;

    public OBB(Vector3dc center, Vector3dc halfSize, Quaternionfc rotation) {
        this.center = center;
        this.halfSize = halfSize;
        this.rotation = rotation;
    }

    public Vector3dc[] getCorners() {
        Vector3dc[] corners = new Vector3dc[8];
        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? -this.halfSize.x() : this.halfSize.x();
            double y = (i & 2) == 0 ? -this.halfSize.y() : this.halfSize.y();
            double z = (i & 4) == 0 ? -this.halfSize.z() : this.halfSize.z();
            Vector3d localCorner = new Vector3d(this.center.x() + x, this.center.y() + y, this.center.z() + z);
            corners[i] = this.rotation.transform(localCorner);
        }
        return corners;
    }

    public Set<Vector3ic> getBlocksInsideApprox() {
        Set<Vector3ic> vectors = new HashSet<>();

        // Start in the negative corner
        Vector3d start = this.rotation.transform(new Vector3d(this.halfSize).negate()).add(this.center);

        Vector3d directionX = this.rotation.transform(new Vector3d(1, 0, 0));
        Vector3d directionY = this.rotation.transform(new Vector3d(0, 1, 0));
        Vector3d directionZ = this.rotation.transform(new Vector3d(0, 0, 1));

        Vector3d deltaX = new Vector3d();
        Vector3d deltaY = new Vector3d();
        Vector3d deltaZ = new Vector3d();

        Vector3d point = new Vector3d();

        for (int localDeltaX = 0; localDeltaX <= 2 * this.halfSize.x(); localDeltaX++) {
            for (int localDeltaY = 0; localDeltaY <= 2 * this.halfSize.y(); localDeltaY++) {
                for (int localDeltaZ = 0; localDeltaZ <= 2 * this.halfSize.z(); localDeltaZ++) {
                    point.set(start).add(deltaX).add(deltaY).add(deltaZ);
                    vectors.add(new Vector3i(point, RoundingMode.FLOOR));
                    deltaZ.add(directionZ);
                }
                deltaZ.zero();
                deltaY.add(directionY);
            }
            deltaY.zero();
            deltaX.add(directionX);
        }
        return vectors;
    }

    public void visualize(ItemDisplay itemDisplay) {
        itemDisplay.setTransformation(
            new Matrix4f()
                .rotate(this.rotation)
                .scale((float) (2 * this.halfSize.x()), (float) (2 * this.halfSize.y()), (float) (2 * this.halfSize.z()))
        );
        itemDisplay.setPosition(new Vector3d(this.center));
    }
}
