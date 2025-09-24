package ca.bkaw.torque.fabric.mixin;

import ca.bkaw.torque.PlatformEvents;
import ca.bkaw.torque.Torque;
import ca.bkaw.torque.fabric.TorqueFabric;
import ca.bkaw.torque.fabric.platform.FabricInteractionEntity;
import ca.bkaw.torque.fabric.platform.FabricPlatform;
import ca.bkaw.torque.fabric.platform.FabricPlayer;
import ca.bkaw.torque.vehicle.Vehicle;
import ca.bkaw.torque.vehicle.VehicleManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Interaction.class)
public class InteractionMixin {
    @Inject(method = "interact", at = @At("HEAD"))
    private void onInteract(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return;
        }
        Torque torque = torqueFabric.getTorque();
        if (torque == null) {
            return;
        }
        VehicleManager vehicleManager = torque.getVehicleManager();
        Vehicle vehicle = vehicleManager.getVehicleFromPart(new FabricInteractionEntity((Interaction) (Object) this));
        if (vehicle == null) {
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
        eventHandler.rightClickVehicle(vehicle, new FabricPlayer(serverPlayer));
    }

    @Inject(method = "skipAttackInteraction", at = @At("HEAD"))
    private void onAttack(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        TorqueFabric torqueFabric = TorqueFabric.getInstance();
        if (torqueFabric == null) {
            return;
        }
        Torque torque = torqueFabric.getTorque();
        if (torque == null) {
            return;
        }
        VehicleManager vehicleManager = torque.getVehicleManager();
        Vehicle vehicle = vehicleManager.getVehicleFromPart(new FabricInteractionEntity((Interaction) (Object) this));
        if (vehicle == null) {
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
        eventHandler.leftClickVehicle(vehicle, new FabricPlayer(serverPlayer));
    }
}