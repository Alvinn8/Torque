package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.DataInput;
import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NbtInputOutput implements DataInput, DataOutput {
    private final CompoundTag nbt;

    public NbtInputOutput(@NotNull CompoundTag nbt) {
        this.nbt = nbt;
    }

    private static String makeKey(String key) {
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
        return this.nbt.read(makeKey(key), ResourceLocation.CODEC)
            .map(id -> new Identifier(id.getNamespace(), id.getPath()))
            .orElse(defaultValue);
    }

    @Override
    public void writeIdentifier(String key, Identifier value) {
        this.nbt.putString(makeKey(key), value.toString());
    }

    @Override
    public Vector3f readVector3f(String key, Vector3f defaultValue) {
        return this.nbt.read(makeKey(key), ExtraCodecs.VECTOR3F).orElse(defaultValue);
    }

    @Override
    public void writeVector3f(String key, Vector3f value) {
        this.nbt.store(makeKey(key), ExtraCodecs.VECTOR3F, value);
    }

    @Override
    public Quaternionf readQuaternionf(String key, Quaternionf defaultValue) {
        return this.nbt.read(makeKey(key), ExtraCodecs.QUATERNIONF_COMPONENTS).orElse(defaultValue);
    }

    @Override
    public void writeQuaternionf(String key, Quaternionf value) {
        this.nbt.store(makeKey(key), ExtraCodecs.QUATERNIONF_COMPONENTS, value);

    }

    @Override
    public DataOutput getOrCreateDataOutput(String key) {
        CompoundTag childNbt = this.nbt.getCompound(makeKey(key)).orElseGet(() -> {
            CompoundTag child = new CompoundTag();
            this.nbt.put(makeKey(key), child);
            return child;
        });
        return new NbtInputOutput(childNbt);
    }

    @Override
    public @NotNull DataInput getDataInput(String key) {
        return this.nbt.getCompound(makeKey(key))
            .map(child -> (DataInput) new NbtInputOutput(child))
            .orElse(DataInput.empty());
    }

    @Override
    public void save() {
        // The data is saved automatically when the entity is saved.
        // So we don't need to do anything here.
    }
}
