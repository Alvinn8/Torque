package ca.bkaw.torque.fabric.mixin;

import ca.bkaw.torque.fabric.FabricTorqueCommand;
import ca.bkaw.torque.fabric.TorqueFabric;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow public abstract CommandDispatcher<CommandSourceStack> getDispatcher();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void registerTorqueCommand(Commands.CommandSelection selection, CommandBuildContext ctx, CallbackInfo ci) {
        TorqueFabric torque = TorqueFabric.getInstance();
        if (torque == null) {
            return;
        }
        FabricTorqueCommand command = torque.getCommand();
        if (command == null) {
            return;
        }
        command.register(this.getDispatcher());
    }
}
