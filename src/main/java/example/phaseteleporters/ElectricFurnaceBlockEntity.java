package example.phaseteleporters;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.BlastingRecipe;
import net.minecraft.recipe.SmeltingRecipe;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PEMachineInventory;

public final class ElectricFurnaceBlockEntity extends PEBlockEntity implements PEMachineInventory, NamedScreenHandlerFactory {
    public static final int PROCESS_TIME = 160;
    public static final long PE_PER_TICK = 20;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private int progress;
    private Identifier currentRecipeId;
    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return index == 0 ? progress : index == 1 ? PROCESS_TIME
                    : index >= 2 && index < 6 ? PEPropertyCodec.part(ElectricFurnaceBlockEntity.this, index - 2) : 0;
        }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int size() { return 6; }
    };

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.ELECTRIC_FURNACE_BLOCK_ENTITY, pos, state, 30_000,
                PESideMode.INPUT, PESideMode.INPUT, PESideMode.ITEM_INPUT,
                PESideMode.ITEM_OUTPUT, PESideMode.ENERGY_ITEM_INPUT,
                PESideMode.ENERGY_ITEM_OUTPUT, PESideMode.DISABLED);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ElectricFurnaceBlockEntity furnace) {
        example.phaseteleporters.energy.PEEnergyItemTransfer.discharge(furnace, furnace.items.get(2));
        furnace.transferConfiguredItems();
        furnace.sendConfiguredOutput();
        if (!furnace.canWork()) return;
        ItemStack input = furnace.items.get(0);
        ItemStack output = furnace.items.get(1);
        RecipeEntry<SmeltingRecipe> smelting = input.isEmpty() ? null : world.getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(input), world).orElse(null);
        RecipeEntry<BlastingRecipe> blasting = input.isEmpty() || smelting != null ? null : world.getRecipeManager()
                .getFirstMatch(RecipeType.BLASTING, new SingleStackRecipeInput(input), world).orElse(null);
        Identifier recipeId = smelting != null ? smelting.id() : blasting != null ? blasting.id() : null;
        if (!java.util.Objects.equals(recipeId, furnace.currentRecipeId)) {
            furnace.currentRecipeId = recipeId;
            furnace.progress = 0;
            furnace.markDirty();
        }
        ItemStack result = smelting != null ? smelting.value().getResult(world.getRegistryManager())
                : blasting != null ? blasting.value().getResult(world.getRegistryManager()) : ItemStack.EMPTY;
        boolean canProcess = !result.isEmpty() && (output.isEmpty()
                || (ItemStack.areItemsAndComponentsEqual(output, result)
                && output.getCount() + result.getCount() <= output.getMaxCount()));
        if (!canProcess) {
            if (furnace.progress != 0) {
                furnace.progress = 0;
                furnace.markDirty();
            }
            return;
        }
        if (furnace.getStored() < PE_PER_TICK) return;
        furnace.extract(PE_PER_TICK, false);
        furnace.progress++;
        if (furnace.progress >= PROCESS_TIME) {
            input.decrement(1);
            if (output.isEmpty()) furnace.items.set(1, result.copy());
            else output.increment(result.getCount());
            furnace.progress = 0;
        }
        furnace.markDirty();
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
    @Override public boolean isValid(int slot, ItemStack stack) {
        return slot == 2 && stack.getItem() instanceof example.phaseteleporters.energy.PEChargeableItem || slot == 0;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items.clear();
        Inventories.readNbt(nbt, items, lookup);
        progress = Math.clamp(nbt.getInt("Progress"), 0, PROCESS_TIME - 1);
        currentRecipeId = nbt.contains("CurrentRecipe") ? Identifier.tryParse(nbt.getString("CurrentRecipe")) : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
        nbt.putInt("Progress", progress);
        if (currentRecipeId != null) nbt.putString("CurrentRecipe", currentRecipeId.toString());
    }

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.electric_furnace"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ElectricFurnaceScreenHandler(syncId, playerInventory, this, properties);
    }
}
