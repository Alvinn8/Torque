package ca.bkaw.torque.platform;

import org.joml.Quaternionf;
import org.joml.Vector3d;

/**
 * An interface for reading structured data.
 */
public interface DataInput {
    Identifier readIdentifier(String key, Identifier defaultValue);
    Vector3d readVector3d(String key, Vector3d defaultValue);
    Quaternionf readQuaternionf(String key, Quaternionf defaultValue);

    static DataInput empty() {
        return new Empty();
    }

    /**
     * An empty implementation of {@link DataInput} that always returns default values.
     */
    class Empty implements DataInput {
        @Override public Identifier readIdentifier(String key, Identifier defaultValue) { return defaultValue; }
        @Override public Vector3d readVector3d(String key, Vector3d defaultValue) { return defaultValue; }
        @Override public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) { return defaultValue; }
    }
}
