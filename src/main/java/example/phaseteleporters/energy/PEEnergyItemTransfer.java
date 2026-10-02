package example.phaseteleporters.energy;

import net.minecraft.item.ItemStack;

/** Transfers only the energy accepted by the destination. */
public final class PEEnergyItemTransfer {
    private PEEnergyItemTransfer() {}

    public static void discharge(PEBlockEntity machine, ItemStack stack) {
        if (!(stack.getItem() instanceof PEChargeableItem item)) return;
        long available = item.extractPE(stack, machine.getCapacity() - machine.getStored(), true);
        long accepted = machine.insert(available, true);
        if (accepted <= 0) return;
        long extracted = item.extractPE(stack, accepted, false);
        long inserted = machine.insert(extracted, false);
        if (inserted < extracted) item.insertPE(stack, extracted - inserted, false);
        if (inserted > 0) machine.markDirty();
    }

    public static void charge(PEBlockEntity machine, ItemStack stack) {
        if (!(stack.getItem() instanceof PEChargeableItem item)) return;
        long offered = machine.extract(machine.getStored(), true);
        long accepted = item.insertPE(stack, offered, true);
        if (accepted <= 0) return;
        long supplied = machine.extract(accepted, false);
        long inserted = item.insertPE(stack, supplied, false);
        if (inserted < supplied) machine.restoreStoredEnergy(supplied - inserted);
        if (inserted > 0) machine.markDirty();
    }
}
