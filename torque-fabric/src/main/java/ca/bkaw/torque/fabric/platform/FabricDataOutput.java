package ca.bkaw.torque.fabric.platform;

import ca.bkaw.torque.platform.DataOutput;
import ca.bkaw.torque.platform.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FabricDataOutput implements DataOutput {
    private final ValueOutput valueOutput;

    public FabricDataOutput(ValueOutput valueOutput) {
        this.valueOutput = valueOutput;
    }

    private static String makeKey(String key) {
        return FabricDataInput.makeKey(key);
    }

    @Override
    public void writeIdentifier(String key, Identifier value) {
        this.valueOutput.putString(makeKey(key), value.toString());
    }

    @Override
    public void writeVector3f(String key, Vector3f value) {
        this.valueOutput.store(makeKey(key), ExtraCodecs.VECTOR3F, value);
    }

    @Override
    public void writeQuaternionf(String key, Quaternionf value) {
        this.valueOutput.store(makeKey(key), ExtraCodecs.QUATERNIONF_COMPONENTS, value);

    }

    @Override
    public DataOutput getOrCreateDataOutput(String key) {
        return new FabricDataOutput(this.valueOutput.child(makeKey(key)));
    }
}
