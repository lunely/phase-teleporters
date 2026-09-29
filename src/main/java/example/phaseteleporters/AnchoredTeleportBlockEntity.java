package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEChargeableItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public abstract class AnchoredTeleportBlockEntity extends PEBlockEntity implements Inventory {
    private final DefaultedList<ItemStack> slots = DefaultedList.ofSize(2, ItemStack.EMPTY);

    protected AnchoredTeleportBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        super(type, pos, state, capacity);
    }

    public boolean hasAnchorUpgrade() { return slots.get(0).isOf(PhaseTeleportersMod.ANCHOR_UPGRADE); }

    public void dischargeEnergyItem() {
        ItemStack stack = slots.get(1);
        if (!(stack.getItem() instanceof PEChargeableItem chargeable)) return;
        long space = getCapacity() - getStored();
        if (space <= 0) return;
        long available = chargeable.extractPE(stack, space, true);
        long accepted = insert(available, true);
        if (accepted <= 0) return;
        long extracted = chargeable.extractPE(stack, accepted, false);
        if (extracted > 0) {
            insert(extracted, false);
            markDirty();
        }
    }

    public void syncAnchor() {
        if (world instanceof ServerWorld serverWorld)
            AnchorChunkState.setActive(serverWorld, pos, hasAnchorUpgrade());
    }

    public void releaseAnchor() {
        if (world instanceof ServerWorld serverWorld)
            AnchorChunkState.setActive(serverWorld, pos, false);
    }

    @Override public int size() { return slots.size(); }
    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean isEmpty() { return slots.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return slots.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(slots, slot, amount);
        if (!removed.isEmpty()) { markDirty(); if (slot == 0) syncAnchor(); }
        return removed;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(slots, slot);
        if (!removed.isEmpty()) { markDirty(); if (slot == 0) syncAnchor(); }
        return removed;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        if (slot < 0 || slot >= size() || (!stack.isEmpty() && !isValid(slot, stack))) return;
        slots.set(slot, stack);
        markDirty();
        if (slot == 0) syncAnchor();
    }
    @Override public void clear() {
        slots.clear();
        markDirty();
        syncAnchor();
    }
    @Override public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player) && super.canPlayerUse(player);
    }

    /** Teleport access is independent of the inventory's interaction distance. */
    public boolean canPlayerTeleport(PlayerEntity player) { return super.canPlayerUse(player); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && stack.isOf(PhaseTeleportersMod.ANCHOR_UPGRADE)
                || slot == 1 && stack.getItem() instanceof PEChargeableItem;
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        slots.clear();
        Inventories.readNbt(nbt, slots, lookup);
        for (int slot = 0; slot < size(); slot++)
            if (!slots.get(slot).isEmpty() && !isValid(slot, slots.get(slot)))
                slots.set(slot, ItemStack.EMPTY);
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, slots, lookup);
    }
}
