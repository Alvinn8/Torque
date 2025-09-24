package ca.bkaw.torque.fabric.mixin;

import ca.bkaw.torque.PlatformEvents;
import ca.bkaw.torque.Torque;
import ca.bkaw.torque.fabric.ItemDisplayAccessor;
import ca.bkaw.torque.fabric.TorqueFabric;
import ca.bkaw.torque.fabric.platform.FabricItemDisplay;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.ItemDisplay.class)
public class ItemDisplayMixin implements ItemDisplayAccessor {
    @Unique
    private static final String BUKKIT_VALUES_KEY = "BukkitValues";
    @Unique
    private static final String TORQUE_VEHICLE_KEY = "torque:vehicle";

    @Unique
    @Nullable
    private CompoundTag torqueData;

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void torque$readAdditionalSaveData(ValueInput valueInput, CallbackInfo ci) {
        valueInput.child(BUKKIT_VALUES_KEY)
            .flatMap(bukkitValues -> bukkitValues.read(TORQUE_VEHICLE_KEY, ExtraCodecs.NBT))
            .ifPresent(tag -> {
                if (tag instanceof CompoundTag compoundTag) {
                    this.torqueData = compoundTag;
                    this.loadVehicleFromData();
                }
            });
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void torque$addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {
        // If this vehicle is a part of a vehicle, save it now so that the data can be
        // serialized now.
        this.saveVehicleData();

        if (this.torqueData != null) {
            ValueOutput bukkitValues = valueOutput.child(BUKKIT_VALUES_KEY);
            bukkitValues.store(TORQUE_VEHICLE_KEY, ExtraCodecs.NBT, this.torqueData);
        }
    }

    @Unique
    private void saveVehicleData() {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return;
        }
        FabricPlatform platform = torqueFabric.getPlatform();
        if (platform == null) {
            return;
        }
        PlatformEvents eventHandler = platform.getEventHandler();
        if (eventHandler == null) {
            return;
        }
        Torque torque = torqueFabric.getTorque();
        if (torque == null) {
            return;
        }
        eventHandler.onItemDisplaySave(new FabricItemDisplay((Display.ItemDisplay) (Object) this));
    }

    @Unique
    private void loadVehicleFromData() {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return;
        }
        PlatformEvents eventHandler = torqueFabric.getPlatform().getEventHandler();
        if (eventHandler == null) {
            return;
        }
        eventHandler.loadVehicleFromData(new FabricItemDisplay((Display.ItemDisplay) (Object) this));
    }

    @Override
    public @Nullable CompoundTag torque$getTorqueData() {
        return this.torqueData;
    }

    @Override
    public void torque$setTorqueData(@Nullable CompoundTag torqueData) {
        this.torqueData = torqueData;
    }
}
