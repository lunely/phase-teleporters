package example.phaseteleports;

import example.phaseteleports.energy.PEBlockEntity;
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
    private final DefaultedList<ItemStack> anchorSlot = DefaultedList.ofSize(1, ItemStack.EMPTY);

    protected AnchoredTeleportBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean hasAnchorUpgrade() { return anchorSlot.get(0).isOf(PhaseTeleportsMod.ANCHOR_UPGRADE); }

    public void syncAnchor() {
        if (world instanceof ServerWorld serverWorld)
            AnchorChunkState.setActive(serverWorld, pos, hasAnchorUpgrade());
    }

    public void releaseAnchor() {
        if (world instanceof ServerWorld serverWorld)
            AnchorChunkState.setActive(serverWorld, pos, false);
    }

    @Override public int size() { return 1; }
    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean isEmpty() { return anchorSlot.get(0).isEmpty(); }
    @Override public ItemStack getStack(int slot) { return anchorSlot.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(anchorSlot, slot, amount);
        if (!removed.isEmpty()) { markDirty(); syncAnchor(); }
        return removed;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(anchorSlot, slot);
        if (!removed.isEmpty()) { markDirty(); syncAnchor(); }
        return removed;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        if (slot != 0 || (!stack.isEmpty() && !stack.isOf(PhaseTeleportsMod.ANCHOR_UPGRADE))) return;
        anchorSlot.set(0, stack);
        markDirty();
        syncAnchor();
    }
    @Override public void clear() {
        anchorSlot.clear();
        markDirty();
        syncAnchor();
    }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && stack.isOf(PhaseTeleportsMod.ANCHOR_UPGRADE);
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        anchorSlot.clear();
        Inventories.readNbt(nbt, anchorSlot, lookup);
        if (!anchorSlot.get(0).isEmpty() && !hasAnchorUpgrade()) anchorSlot.clear();
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, anchorSlot, lookup);
    }
}
