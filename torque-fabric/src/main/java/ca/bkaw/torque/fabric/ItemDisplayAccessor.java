package ca.bkaw.torque.fabric;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public interface ItemDisplayAccessor {
    @Nullable
    CompoundTag torque$getTorqueData();

    void torque$setTorqueData(@Nullable CompoundTag torqueData);
}
