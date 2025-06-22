package ca.bkaw.torque.paper.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import java.util.List;

public class PdcInputOutput implements DataInput, DataOutput {
    private final PersistentDataContainer pdc;

    public PdcInputOutput(PersistentDataContainer pdc) {
        this.pdc = pdc;
    }

    private NamespacedKey makeKey(String key) {
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
    public Vector3d readVector3d(String key, Vector3d defaultValue) {
        List<Double> data = this.pdc.get(this.makeKey(key), PersistentDataType.LIST.doubles());
        if (data == null || data.size() != 3) {
            return defaultValue;
        }
        return new Vector3d(data.get(0), data.get(1), data.get(2));
    }

    @Override
    public void writeVector3d(String key, Vector3d value) {
        this.pdc.set(this.makeKey(key), PersistentDataType.LIST.doubles(),
            List.of(value.x(), value.y(), value.z())
        );
    }

    @Override
    public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) {
        List<Float> data = this.pdc.get(this.makeKey(key), PersistentDataType.LIST.floats());
        return new Quaternionf(data.get(0), data.get(1), data.get(2), data.get(3));
    }

    @Override
    public void writeQuaternionf(String key, Quaternionf value) {
        this.pdc.set(this.makeKey(key), PersistentDataType.LIST.floats(),
            List.of(value.x(), value.y(), value.z(), value.w())
        );
    }
}
