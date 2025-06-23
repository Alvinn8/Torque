package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.Identifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FabricDataInput implements DataInput {
    private final ValueInput valueInput;

    public FabricDataInput(ValueInput valueInput) {
        this.valueInput = valueInput;
    }

    static String makeKey(String key) {
        if (Identifier.validIdentifier(key)) {
            return key;
        }
        if (!Identifier.validKey(key)) {
            throw new IllegalArgumentException(key + " is not a valid key for persistent data");
        }
        // To keep compatibility with Bukkit PersistentDataContainer, which always uses
        // namespaced keys (identifiers), we also use the same format.
        return "data:" + key;
    }

    @Override
    public Identifier readIdentifier(String key, Identifier defaultValue) {
        return this.valueInput.read(makeKey(key), ResourceLocation.CODEC)
            .map(id -> new Identifier(id.getNamespace(), id.getPath()))
            .orElse(defaultValue);
    }

    @Override
    public Vector3f readVector3f(String key, Vector3f defaultValue) {
        return this.valueInput.read(makeKey(key), ExtraCodecs.VECTOR3F).orElse(defaultValue);
    }

    @Override
    public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) {
        return this.valueInput.read(makeKey(key), ExtraCodecs.QUATERNIONF_COMPONENTS).orElse(defaultValue);
    }

    @Override
    public @NotNull DataInput getDataInput(String key) {
        return this.valueInput.child(makeKey(key))
            .map(child -> (DataInput) new FabricDataInput(child))
            .orElse(DataInput.empty());
    }

}
