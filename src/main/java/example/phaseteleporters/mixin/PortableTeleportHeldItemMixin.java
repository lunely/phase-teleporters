package example.phaseteleporters.mixin;

import example.phaseteleporters.PhaseTeleportersMod;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(HeldItemRenderer.class)
public abstract class PortableTeleportHeldItemMixin {
    @Redirect(method = "updateHeldItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;areEqual(Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)Z"))
    private boolean phaseteleporters$ignorePortableDataChange(ItemStack rendered, ItemStack held) {
        if (rendered.isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)
                && held.isOf(PhaseTeleportersMod.PORTABLE_TELEPORT))
            return ItemStack.areItemsEqual(rendered, held)
                    && rendered.getCount() == held.getCount();
        return ItemStack.areEqual(rendered, held);
    }
}
