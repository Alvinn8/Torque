package ca.bkaw.torque.platform;

import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * An interface for writing structured data.
 */
public interface DataOutput {
    void writeIdentifier(String key, Identifier value);
    void writeVector3d(String key, Vector3d value);
    void writeQuaternionf(String key, Quaternionf value);
}
