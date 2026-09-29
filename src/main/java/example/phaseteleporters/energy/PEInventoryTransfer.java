package example.phaseteleporters.energy;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Moves one item per configured face between a machine and an adjacent inventory. */
public final class PEInventoryTransfer {
    private PEInventoryTransfer() {}

    public static void tick(PEBlockEntity machine) {
        World world = machine.getWorld();
        if (world == null || world.isClient || !(machine instanceof Inventory inventory)) return;
        long transferPhase = world.getTime() % 5;
        if (transferPhase != 0 && transferPhase != 2) return;

        for (Direction side : Direction.values()) {
            PESideMode mode = machine.getSideMode(side);
            if (!mode.allowsItemInput() && !mode.allowsItemOutput()) continue;
            BlockPos neighborPos = machine.getPos().offset(side);
            if (!world.isChunkLoaded(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) continue;
            BlockEntity neighbor = world.getBlockEntity(neighborPos);
            if (!(neighbor instanceof Inventory adjacent)) continue;
            Direction neighborFace = side.getOpposite();
            if (neighbor instanceof PEBlockEntity other) {
                PESideMode neighborMode = other.getSideMode(neighborFace);
                if (mode.allowsItemInput() ? !neighborMode.allowsItemOutput()
                        : !neighborMode.allowsItemInput()) continue;
            }
            if (mode.allowsItemInput())
                move(adjacent, inventory, neighborFace, side, neighbor, machine);
            else
                move(inventory, adjacent, side, neighborFace, machine, neighbor);
        }
    }

    private static void move(Inventory source, Inventory target, Direction sourceFace,
            Direction targetFace, BlockEntity sourceBlock, BlockEntity targetBlock) {
        int[] sourceSlots = slots(source, sourceBlock, sourceFace, false);
        int[] targetSlots = slots(target, targetBlock, targetFace, true);
        for (int sourceSlot : sourceSlots) {
            ItemStack stack = source.getStack(sourceSlot);
            if (stack.isEmpty() || !canExtract(source, sourceSlot, stack, sourceFace)) continue;
            for (int targetSlot : targetSlots) {
                if (!target.isValid(targetSlot, stack)
                        || !canInsert(target, targetSlot, stack, targetFace)) continue;
                ItemStack existing = target.getStack(targetSlot);
                if (!existing.isEmpty() && (!ItemStack.areItemsAndComponentsEqual(existing, stack)
                        || existing.getCount() >= existing.getMaxCount())) continue;
                ItemStack moved = source.removeStack(sourceSlot, 1);
                if (moved.isEmpty()) return;
                if (existing.isEmpty()) target.setStack(targetSlot, moved);
                else {
                    existing.increment(1);
                    target.markDirty();
                }
                source.markDirty();
                return;
            }
        }
    }

    private static int[] slots(Inventory inventory, BlockEntity block, Direction face, boolean input) {
        if (block instanceof PEBlockEntity machine)
            return input ? machine.itemInputSlots() : machine.itemOutputSlots();
        if (inventory instanceof SidedInventory sided) return sided.getAvailableSlots(face);
        int[] slots = new int[inventory.size()];
        for (int i = 0; i < slots.length; i++) slots[i] = i;
        return slots;
    }

    private static boolean canExtract(Inventory inventory, int slot, ItemStack stack, Direction face) {
        return !(inventory instanceof SidedInventory sided) || sided.canExtract(slot, stack, face);
    }

    private static boolean canInsert(Inventory inventory, int slot, ItemStack stack, Direction face) {
        return !(inventory instanceof SidedInventory sided) || sided.canInsert(slot, stack, face);
    }
}
