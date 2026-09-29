package example.phaseteleporters.energy;

import net.minecraft.item.ItemStack;

/** Moves PE between item stacks without exceeding either item's available amount or capacity. */
public final class PEItemTransfer {
    private PEItemTransfer() {}

    public static long transfer(ItemStack source, ItemStack target, long limit) {
        if (source == target || source.isEmpty() || target.isEmpty() || limit <= 0
                || !(source.getItem() instanceof PEChargeableItem sourceItem)
                || !(target.getItem() instanceof PEChargeableItem targetItem)) return 0;
        long available = sourceItem.extractPE(source, limit, true);
        long accepted = targetItem.insertPE(target, available, true);
        if (accepted <= 0) return 0;
        long extracted = sourceItem.extractPE(source, accepted, false);
        long inserted = targetItem.insertPE(target, extracted, false);
        if (inserted < extracted) sourceItem.insertPE(source, extracted - inserted, false);
        return inserted;
    }
}
