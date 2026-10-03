package example.phaseteleporters.energy;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Shared persistent charging and discharging slots for PE cubes. */
public abstract class PEChargingCubeBlockEntity extends PEBlockEntity implements Inventory {
    private final DefaultedList<ItemStack> energyItems = DefaultedList.ofSize(2, ItemStack.EMPTY);

    protected PEChargingCubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, PESideMode.INPUT_OUTPUT,
                PESideMode.INPUT, PESideMode.OUTPUT, PESideMode.INPUT_OUTPUT, PESideMode.DISABLED);
    }

    protected PEChargingCubeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        super(type, pos, state, capacity, PESideMode.INPUT_OUTPUT,
                PESideMode.INPUT, PESideMode.OUTPUT, PESideMode.INPUT_OUTPUT, PESideMode.DISABLED);
    }

    protected void chargeItem() {
        chargeItem(0);
    }

    protected void chargeItem(int slot) {
        ItemStack stack = energyItems.get(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof PEChargeableItem chargeable)) return;
        long space = Math.max(0, chargeable.getCapacityPE(stack) - chargeable.getStoredPE(stack));
        if (space == 0) return;
        long accepted = chargeable.insertPE(stack, Math.min(space, getStored()), true);
        if (accepted <= 0) return;
        long supplied = extract(accepted, false);
        long inserted = chargeable.insertPE(stack, supplied, false);
        if (inserted < supplied) insert(supplied - inserted, false);
        if (inserted > 0) markDirty();
    }

    protected void dischargeItem() {
        ItemStack stack = energyItems.get(1);
        if (stack.isEmpty() || !(stack.getItem() instanceof PEChargeableItem chargeable)) return;
        long space = Math.max(0, getCapacity() - getStored());
        if (space == 0) return;
        long available = chargeable.extractPE(stack, space, true);
        long accepted = insert(available, true);
        if (accepted <= 0) return;
        long extracted = chargeable.extractPE(stack, accepted, false);
        long inserted = insert(extracted, false);
        if (inserted < extracted) chargeable.insertPE(stack, extracted - inserted, false);
        if (inserted > 0) markDirty();
    }

    @Override public int size() { return energyItems.size(); }
    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean isEmpty() { return energyItems.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return energyItems.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(energyItems, slot, amount);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(energyItems, slot);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size() || (!stack.isEmpty() && !isValid(slot, stack))) return;
        energyItems.set(slot, stack);
        markDirty();
    }
    @Override public void clear() { energyItems.clear(); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return super.canPlayerUse(player) && Inventory.canPlayerUse(this, player); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < size() && stack.getItem() instanceof PEChargeableItem;
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        energyItems.clear();
        Inventories.readNbt(nbt, energyItems, lookup);
        for (int slot = 0; slot < size(); slot++) {
            if (!energyItems.get(slot).isEmpty() && !isValid(slot, energyItems.get(slot)))
                energyItems.set(slot, ItemStack.EMPTY);
        }
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, energyItems, lookup);
    }
}
