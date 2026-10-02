package example.phaseteleporters.mixin;

import example.phaseteleporters.PortableModePayload;
import example.phaseteleporters.PortableTeleportItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class PortableTeleportScrollMixin {
    @Shadow @Final private MinecraftClient client;
    @Unique private double phaseteleporters$scroll;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void phaseteleporters$switchPortableMode(long window, double horizontal,
            double vertical, CallbackInfo ci) {
        if (window != client.getWindow().getHandle() || client.currentScreen != null
                || client.player == null || !client.options.sneakKey.isPressed()
                || !(client.player.getMainHandStack().getItem() instanceof PortableTeleportItem portable)
                || !ClientPlayNetworking.canSend(PortableModePayload.ID)) {
            phaseteleporters$scroll = 0;
            return;
        }
        // Only the wheel is consumed; vanilla continues to handle sneak.
        ci.cancel();
        if (vertical == 0) return;
        if (Math.signum(vertical) != Math.signum(phaseteleporters$scroll))
            phaseteleporters$scroll = 0;
        phaseteleporters$scroll += vertical;
        int steps = (int) phaseteleporters$scroll;
        phaseteleporters$scroll -= steps;
        ItemStack stack = client.player.getMainHandStack();
        for (int i = 0; i < Math.min(Math.abs(steps), 20); i++) {
            var mode = portable.getMode(stack).step(steps > 0 ? 1 : -1);
            portable.setMode(stack, mode);
            ClientPlayNetworking.send(new PortableModePayload(client.player.getInventory().selectedSlot, mode.ordinal()));
        }
    }
}
