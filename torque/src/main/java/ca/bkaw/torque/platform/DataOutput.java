package ca.bkaw.torque.platform;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An interface for writing structured data.
 */
public interface DataOutput {
    void writeIdentifier(String key, Identifier value);
    void writeVector3f(String key, Vector3f value);
    void writeQuaternionf(String key, Quaternionf value);
    void writeFloat(String key, float value);

    /**
     * Get or create a nested {@link DataOutput} for the given key.
     *
     * @param key The key for the nested data.
     * @return The nested {@link DataOutput}, or a new one if it does not exist.
     */
    DataOutput getOrCreateDataOutput(String key);

    /**
     * Save the data to the underlying storage.
     * <p>
     * Must be called for each {@link DataOutput}.
     * Remember to save nested {@link DataOutput}s first.
     */
    void save();
}
