package example.phaseteleporters.mixin;

import example.phaseteleporters.PortableModePayload;
import example.phaseteleporters.PortableTeleportItem;
import example.phaseteleporters.ColorConfiguratorItem;
import example.phaseteleporters.ColorConfiguratorModePayload;
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
    @Unique private net.minecraft.item.Item phaseteleporters$scrollItem;

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void phaseteleporters$switchPortableMode(long window, double horizontal,
            double vertical, CallbackInfo ci) {
        if (window != client.getWindow().getHandle() || client.currentScreen != null
                || client.player == null || !client.options.sneakKey.isPressed()) {
            phaseteleporters$scroll = 0;
            phaseteleporters$scrollItem = null;
            return;
        }
        ItemStack stack = client.player.getMainHandStack();
        boolean portableTarget = stack.getItem() instanceof PortableTeleportItem
                && ClientPlayNetworking.canSend(PortableModePayload.ID);
        boolean configuratorTarget = stack.getItem() instanceof ColorConfiguratorItem
                && ClientPlayNetworking.canSend(ColorConfiguratorModePayload.ID);
        if (!portableTarget && !configuratorTarget) {
            phaseteleporters$scroll = 0;
            phaseteleporters$scrollItem = null;
            return;
        }
        if (phaseteleporters$scrollItem != stack.getItem()) phaseteleporters$scroll = 0;
        phaseteleporters$scrollItem = stack.getItem();
        // Only the wheel is consumed; vanilla continues to handle sneak.
        ci.cancel();
        if (vertical == 0) return;
        if (Math.signum(vertical) != Math.signum(phaseteleporters$scroll))
            phaseteleporters$scroll = 0;
        phaseteleporters$scroll += vertical;
        int steps = (int) phaseteleporters$scroll;
        phaseteleporters$scroll -= steps;
        for (int i = 0; i < Math.min(Math.abs(steps), 20); i++) {
            if (portableTarget) {
                var portable = (PortableTeleportItem) stack.getItem();
                var mode = portable.getMode(stack).step(steps > 0 ? 1 : -1);
                portable.setMode(stack, mode);
                ClientPlayNetworking.send(new PortableModePayload(client.player.getInventory().selectedSlot, mode.ordinal()));
            } else {
                var mode = ColorConfiguratorItem.mode(stack).step(steps > 0 ? 1 : -1);
                ColorConfiguratorItem.setMode(stack, mode);
                ClientPlayNetworking.send(new ColorConfiguratorModePayload(client.player.getInventory().selectedSlot, mode.ordinal()));
            }
        }
    }
}
