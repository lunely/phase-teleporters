package example.phaseteleporters.energy;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** One rechargeable item, separate from recipe and automation slots. */
public final class PEEnergyItemSlot extends Slot {
    public PEEnergyItemSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override public boolean canInsert(ItemStack stack) {
        return stack.getItem() instanceof PEChargeableItem;
    }

    @Override public int getMaxItemCount() { return 1; }
}
