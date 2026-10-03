package example.phaseteleporters;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;

public final class PortableTeleportScreenHandler extends ScreenHandler {
    private final Hand usedHand;

    public PortableTeleportScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null);
    }

    public PortableTeleportScreenHandler(int syncId, PlayerInventory inventory, Hand usedHand) {
        super(PhaseTeleportersMod.PORTABLE_TELEPORT_SCREEN_HANDLER, syncId);
        this.usedHand = usedHand;
    }

    public ItemStack getUsedStack(PlayerEntity player) {
        if (usedHand == null) return ItemStack.EMPTY;
        ItemStack stack = player.getStackInHand(usedHand);
        return stack.isOf(PhaseTeleportersMod.PORTABLE_TELEPORT) ? stack : ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (usedHand != null) return !getUsedStack(player).isEmpty();
        return player.getMainHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)
                || player.getOffHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
