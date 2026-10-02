package example.phaseteleporters.mixin;

import example.phaseteleporters.EmergencyTeleportRescue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EmergencyFallMovementMixin {
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void phaseteleporters$rescueBeforeLanding(MovementType type, Vec3d movement, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player
                && EmergencyTeleportRescue.beforeFallMovement(player, movement)) {
            player.setVelocity(Vec3d.ZERO);
            player.fallDistance = 0;
            ci.cancel();
        }
    }
}
