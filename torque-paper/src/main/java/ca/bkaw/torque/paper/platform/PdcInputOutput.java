package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class PdcInputOutput implements DataInput, DataOutput {
    private final PersistentDataContainer pdc;
    private final PersistentDataContainer parentPdc;
    private final NamespacedKey selfKey;

    public PdcInputOutput(PersistentDataContainer pdc, PersistentDataContainer parentPdc, NamespacedKey selfKey) {
        this.pdc = pdc;
        this.parentPdc = parentPdc;
        this.selfKey = selfKey;
    }

    private NamespacedKey makeKey(String key) {
        if (Identifier.validIdentifier(key)) {
            return NamespacedKey.fromString(key);
        }
        // While this can be considered bad practice, since we always store data
        // in its own data container, we can avoid collisions with other plugins.
        return new NamespacedKey("data", key);
    }

    @Override
    public Identifier readIdentifier(String key, Identifier defaultValue) {
        String string = this.pdc.get(this.makeKey(key), PersistentDataType.STRING);
        if (string == null) {
            return defaultValue;
        }
        return Identifier.fromString(string);
    }

    @Override
    public void writeIdentifier(String key, Identifier value) {
        this.pdc.set(this.makeKey(key), PersistentDataType.STRING, value.toString());
    }

    @Override
    public Vector3f readVector3f(String key, Vector3f defaultValue) {
        List<Float> data = this.pdc.get(this.makeKey(key), PersistentDataType.LIST.floats());
        if (data == null || data.size() != 3) {
            return defaultValue;
        }
        return new Vector3f(data.get(0), data.get(1), data.get(2));
    }

    @Override
    public void writeVector3f(String key, Vector3f value) {
        this.pdc.set(this.makeKey(key), PersistentDataType.LIST.floats(),
            List.of(value.x(), value.y(), value.z())
        );
    }

    @Override
    public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) {
        List<Float> data = this.pdc.get(this.makeKey(key), PersistentDataType.LIST.floats());
        if (data == null || data.size() != 4) {
            return defaultValue;
        }
        return new Quaternionf(data.get(0), data.get(1), data.get(2), data.get(3));
    }

    @Override
    public void writeQuaternionf(String key, Quaternionf value) {
        this.pdc.set(this.makeKey(key), PersistentDataType.LIST.floats(),
            List.of(value.x(), value.y(), value.z(), value.w())
        );
    }

    @Override
    public @NotNull DataInput getDataInput(String key) {
        PersistentDataContainer nestedPdc = this.pdc.get(this.makeKey(key), PersistentDataType.TAG_CONTAINER);
        if (nestedPdc == null) {
            return DataInput.empty();
        }
        return new PdcInputOutput(nestedPdc, this.pdc, this.makeKey(key));
    }

    @Override
    public DataOutput getOrCreateDataOutput(String key) {
        PersistentDataContainer nestedPdc = this.pdc.get(this.makeKey(key), PersistentDataType.TAG_CONTAINER);
        if (nestedPdc == null) {
            nestedPdc = this.pdc.getAdapterContext().newPersistentDataContainer();
            this.pdc.set(this.makeKey(key), PersistentDataType.TAG_CONTAINER, nestedPdc);
        }
        return new PdcInputOutput(nestedPdc, this.pdc, this.makeKey(key));
    }

    public void save() {
        this.parentPdc.set(this.selfKey, PersistentDataType.TAG_CONTAINER, this.pdc);
    }
}
