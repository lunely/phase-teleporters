package example.phaseteleporters;

import example.phaseteleporters.energy.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public final class SolarPanelBlockEntity extends PEBlockEntity implements NamedScreenHandlerFactory, PEMachineInventory {
    public static final long CAPACITY = 5_000;
    public static final int CLEAR_GENERATION = 250;
    public static final int RAIN_GENERATION = 110;
    public static final long MAX_OUTPUT_PER_TICK = 250;
    private long outputTick = Long.MIN_VALUE;
    private long outputThisTick;
    private int generation;
    private final DefaultedList<ItemStack> energyItems = DefaultedList.ofSize(1, ItemStack.EMPTY);

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.SOLAR_PANEL_BLOCK_ENTITY, pos, state, CAPACITY,
                PESideMode.OUTPUT, PESideMode.OUTPUT, PESideMode.DISABLED);
    }
    @Override protected long getMaxInputPerTick() { return CLEAR_GENERATION; }
    @Override public long extract(long amount, boolean simulate) {
        if (world != null && outputTick != world.getTime()) {
            outputTick = world.getTime();
            outputThisTick = 0;
        }
        long sent = super.extract(Math.min(amount, MAX_OUTPUT_PER_TICK - outputThisTick), simulate);
        if (!simulate) outputThisTick += sent;
        return sent;
    }
    @Override public Runnable createEnergySnapshot() {
        Runnable energy = super.createEnergySnapshot();
        long tick = outputTick, sent = outputThisTick;
        return () -> { energy.run(); outputTick = tick; outputThisTick = sent; };
    }
    static int generation(World world, BlockPos pos) {
        long daytime = Math.floorMod(world.getTimeOfDay(), 24_000L);
        if (!world.getDimension().hasSkyLight() || world.getDimension().hasCeiling()
                || daytime >= 12_000 || !world.isSkyVisible(pos.up())) return 0;
        return world.isRaining() ? RAIN_GENERATION : CLEAR_GENERATION;
    }
    public static void tick(World world, BlockPos pos, BlockState state, SolarPanelBlockEntity panel) {
        panel.generation = panel.canWork() ? generation(world, pos) : 0;
        if (panel.generation > 0) panel.insert(panel.generation, false);
        if (panel.canWork()) panel.chargeItem();
        panel.sendConfiguredOutput();
    }
    private void chargeItem() {
        ItemStack stack = energyItems.get(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof PEChargeableItem item)) return;
        long offered = extract(MAX_OUTPUT_PER_TICK, true);
        long accepted = item.insertPE(stack, offered, true);
        if (accepted <= 0) return;
        long supplied = extract(accepted, false);
        long inserted = item.insertPE(stack, supplied, false);
        if (inserted < supplied) restoreStoredEnergy(supplied - inserted);
        if (inserted > 0) markDirty();
    }
    @Override public int size() { return 1; }
    @Override public int getMaxCountPerStack() { return 1; }
    @Override public boolean isEmpty() { return energyItems.get(0).isEmpty(); }
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
        if (slot != 0 || (!stack.isEmpty() && !isValid(slot, stack))) return;
        stack.setCount(Math.min(stack.getCount(), 1));
        energyItems.set(slot, stack);
        markDirty();
    }
    @Override public void clear() { energyItems.clear(); markDirty(); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof PEChargeableItem;
    }
    @Override public boolean canPlayerUse(PlayerEntity player) {
        return super.canPlayerUse(player) && Inventory.canPlayerUse(this, player);
    }
    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        energyItems.clear();
        Inventories.readNbt(nbt, energyItems, lookup);
        if (!energyItems.get(0).isEmpty() && !isValid(0, energyItems.get(0))) energyItems.set(0, ItemStack.EMPTY);
    }
    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, energyItems, lookup);
    }
    public PropertyDelegate properties() {
        return new PropertyDelegate() {
            @Override public int get(int index) { return index < 4 ? PEPropertyCodec.part(SolarPanelBlockEntity.this, index) : generation; }
            @Override public void set(int index, int value) {}
            @Override public int size() { return 5; }
        };
    }
    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.solar_panel"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new SolarPanelScreenHandler(syncId, inventory, this, properties());
    }
}
