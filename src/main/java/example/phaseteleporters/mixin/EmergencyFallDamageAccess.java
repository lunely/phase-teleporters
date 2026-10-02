package example.phaseteleporters.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface EmergencyFallDamageAccess {
    @Invoker("computeFallDamage") int phaseteleporters$fallDamage(float distance, float multiplier);
}
