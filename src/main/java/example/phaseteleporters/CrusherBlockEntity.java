package example.phaseteleporters;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PEMachineInventory;
import java.util.List;

public final class CrusherBlockEntity extends PEBlockEntity implements PEMachineInventory, NamedScreenHandlerFactory {
    public static final int PROCESS_TIME = 100;
    public static final long PE_PER_TICK = 50;
    public record CrusherRecipe(Item input, Item output) {}
    public static final List<CrusherRecipe> RECIPES = List.of(
            new CrusherRecipe(Items.OBSIDIAN, PhaseTeleportersMod.OBSIDIAN_DUST));
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private int progress;
    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return index == 0 ? progress : index == 1 ? PROCESS_TIME
                    : index >= 2 && index < 6 ? PEPropertyCodec.part(CrusherBlockEntity.this, index - 2) : 0;
        }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int size() { return 6; }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.CRUSHER_BLOCK_ENTITY, pos, state, 30_000,
                PESideMode.INPUT, PESideMode.INPUT, PESideMode.ITEM_INPUT,
                PESideMode.ITEM_OUTPUT, PESideMode.ENERGY_ITEM_INPUT,
                PESideMode.ENERGY_ITEM_OUTPUT, PESideMode.DISABLED);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CrusherBlockEntity crusher) {
        example.phaseteleporters.energy.PEEnergyItemTransfer.discharge(crusher, crusher.items.get(2));
        crusher.transferConfiguredItems();
        crusher.sendConfiguredOutput();
        if (!crusher.canWork()) return;
        ItemStack output = crusher.items.get(1);
        CrusherRecipe recipe = recipeFor(crusher.items.get(0));
        boolean canProcess = recipe != null
                && (output.isEmpty() || (output.isOf(recipe.output())
                && output.getCount() < output.getMaxCount()));
        if (!canProcess) {
            if (crusher.progress != 0) {
                crusher.progress = 0;
                crusher.markDirty();
            }
            return;
        }
        if (crusher.getStored() < PE_PER_TICK) return;
        crusher.extract(PE_PER_TICK, false);
        crusher.progress++;
        if (crusher.progress >= PROCESS_TIME) {
            crusher.items.get(0).decrement(1);
            if (output.isEmpty()) crusher.items.set(1, new ItemStack(recipe.output()));
            else output.increment(1);
            crusher.progress = 0;
        }
        crusher.markDirty();
    }

    private static CrusherRecipe recipeFor(ItemStack input) {
        for (CrusherRecipe recipe : RECIPES)
            if (input.isOf(recipe.input())) return recipe;
        return null;
    }

    @Override public int size() { return items.size(); }
    @Override public int[] itemInputSlots() { return new int[] {0}; }
    @Override public int[] itemOutputSlots() { return new int[] {1}; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(items, slot, amount);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(items, slot);
        if (!removed.isEmpty()) markDirty();
        return removed;
    }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); markDirty(); }
    @Override public void clear() { items.clear(); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == 2 && stack.getItem() instanceof example.phaseteleporters.energy.PEChargeableItem || slot == 0 && recipeFor(stack) != null; }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items.clear();
        Inventories.readNbt(nbt, items, lookup);
        progress = Math.clamp(nbt.getInt("Progress"), 0, PROCESS_TIME - 1);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
        nbt.putInt("Progress", progress);
    }

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.crusher"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CrusherScreenHandler(syncId, playerInventory, this, properties);
    }
}
