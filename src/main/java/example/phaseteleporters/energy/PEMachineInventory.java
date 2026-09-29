package example.phaseteleporters.energy;

import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

/** Makes item ports visible to vanilla hoppers only on their configured faces. */
public interface PEMachineInventory extends SidedInventory {
    @Override
    default int[] getAvailableSlots(Direction side) {
        PEBlockEntity machine = (PEBlockEntity) this;
        return switch (machine.getSideMode(side)) {
            case ITEM_INPUT, ENERGY_ITEM_INPUT -> machine.itemInputSlots();
            case ITEM_OUTPUT, ENERGY_ITEM_OUTPUT -> machine.itemOutputSlots();
            default -> new int[0];
        };
    }

    @Override
    default boolean canInsert(int slot, ItemStack stack, Direction side) {
        PEBlockEntity machine = (PEBlockEntity) this;
        return machine.getSideMode(side).allowsItemInput()
                && contains(machine.itemInputSlots(), slot) && isValid(slot, stack);
    }

    @Override
    default boolean canExtract(int slot, ItemStack stack, Direction side) {
        PEBlockEntity machine = (PEBlockEntity) this;
        return machine.getSideMode(side).allowsItemOutput()
                && contains(machine.itemOutputSlots(), slot);
    }

    private static boolean contains(int[] slots, int slot) {
        for (int candidate : slots) if (candidate == slot) return true;
        return false;
    }
}
