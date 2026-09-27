package example.phaseteleports.energy;

import net.minecraft.item.ItemStack;

/** Implemented by items that can receive PE in an energy cube's charging slot. */
public interface PEChargeableItem {
    long getStoredPE(ItemStack stack);
    long getCapacityPE(ItemStack stack);
    long insertPE(ItemStack stack, long amount, boolean simulate);
    default long extractPE(ItemStack stack, long amount, boolean simulate) { return 0; }
}
