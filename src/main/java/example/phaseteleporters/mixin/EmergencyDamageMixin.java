package example.phaseteleporters.mixin;

import example.phaseteleporters.EmergencyTeleportRescue;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PlayerEntity.class)
public abstract class EmergencyDamageMixin {
    @ModifyArgs(method = "applyDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;setHealth(F)V"))
    private void phaseteleporters$rescueBeforeDeath(Args args, DamageSource source, float amount) {
        if ((Object) this instanceof ServerPlayerEntity player)
            args.set(0, EmergencyTeleportRescue.beforeHealthDamage(player, source, args.get(0)));
    }
}
