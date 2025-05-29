package ca.bkaw.torque.fabric.mixin;

import ca.bkaw.torque.fabric.TorqueFabric;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "runServer", at = @At("HEAD"))
    public void serverStarting(CallbackInfo ci) {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric != null) {
            torqueFabric.setServer((MinecraftServer) (Object) this);
        }
    }
}
