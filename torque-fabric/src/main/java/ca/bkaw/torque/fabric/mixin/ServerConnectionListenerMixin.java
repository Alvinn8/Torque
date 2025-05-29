package ca.bkaw.torque.fabric.mixin;

import ca.bkaw.torque.fabric.TorqueFabric;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/server/network/ServerConnectionListener$1")
public class ServerConnectionListenerMixin {
    @Inject(method = "initChannel", at = @At("TAIL"))
    public void afterInitChannel(Channel channel, CallbackInfo ci) {
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return;
        }
        FabricPlatform platform = torqueFabric.getPlatform();
        if (platform == null) {
            return;
        }
        platform.getChannelHandlers().forEach(((handlerKey, channelHandler) ->
            channel.pipeline().addFirst(handlerKey.toString(), channelHandler)
        ));
    }
}
