package ca.bkaw.torque.platform;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * An interface for reading structured data.
 */
public interface DataInput {
    Identifier readIdentifier(String key, Identifier defaultValue);
    Vector3f readVector3f(String key, Vector3f defaultValue);
    Quaternionf readQuaternionf(String key, Quaternionf defaultValue);
    float readFloat(String key, float defaultValue);

    /**
     * Get or create a nested {@link DataInput} for the given key.
     * <p>
     * If the key does not exist, an empty {@link DataInput} will be returned.
     *
     * @param key The key.
     * @return The nested {@link DataInput}, or an empty one.
     */
    @NotNull
    DataInput getDataInput(String key);

    static DataInput empty() {
        return new Empty();
    }

    /**
     * An empty implementation of {@link DataInput} that always returns default values.
     */
    class Empty implements DataInput {
        @Override public Identifier readIdentifier(String key, Identifier defaultValue) { return defaultValue; }
        @Override public Vector3f readVector3f(String key, Vector3f defaultValue) { return defaultValue; }
        @Override public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) { return defaultValue; }
        @Override public @NotNull DataInput getDataInput(String key) { return DataInput.empty(); }
        @Override public float readFloat(String key, float defaultValue) { return defaultValue; }
    }
}
