package example.phaseteleporters;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public final class PortableTeleportScreenHandler extends ScreenHandler {
    private final Hand usedHand;

    public PortableTeleportScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null);
    }

    public PortableTeleportScreenHandler(int syncId, PlayerInventory inventory, Hand usedHand) {
        super(PhaseTeleportersMod.PORTABLE_TELEPORT_SCREEN_HANDLER, syncId);
        this.usedHand = usedHand;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9,
                        8 + col * 18, 155 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col, 8 + col * 18, 213));
    }

    public ItemStack getUsedStack(PlayerEntity player) {
        if (usedHand == null) return ItemStack.EMPTY;
        ItemStack stack = player.getStackInHand(usedHand);
        return stack.isOf(PhaseTeleportersMod.PORTABLE_TELEPORT) ? stack : ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.getMainHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT)
                || player.getOffHandStack().isOf(PhaseTeleportersMod.PORTABLE_TELEPORT);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        if (slot < 0 || slot >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(slot);
        if (!source.hasStack()) return ItemStack.EMPTY;
        ItemStack original = source.getStack();
        ItemStack copy = original.copy();
        if (!insertItem(original, slot < 27 ? 27 : 0, slot < 27 ? 36 : 27, false))
            return ItemStack.EMPTY;
        if (original.isEmpty()) source.setStack(ItemStack.EMPTY);
        else source.markDirty();
        return copy;
    }
}
