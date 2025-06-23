package ca.bkaw.torque.fabric.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.ItemDisplay.class)
public class ItemDisplayMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void torque$readAdditionalSaveData(ValueInput valueInput, CallbackInfo ci) {

    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void torque$addAdditionalSaveData(ValueOutput valueOutput, CallbackInfo ci) {

    }
}
