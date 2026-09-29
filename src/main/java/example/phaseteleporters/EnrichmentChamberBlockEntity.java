package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PEMachineInventory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeEntry;
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

import java.util.Objects;

public final class EnrichmentChamberBlockEntity extends PEBlockEntity
        implements PEMachineInventory, NamedScreenHandlerFactory {
    public static final int PROCESS_TIME = 100;
    public static final long PE_PER_TICK = 60;

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(2, ItemStack.EMPTY);
    private int progress;
    private Identifier currentRecipeId;
    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return index == 0 ? progress : index == 1 ? PROCESS_TIME
                    : index >= 2 && index < 6
                    ? PEPropertyCodec.part(EnrichmentChamberBlockEntity.this, index - 2) : 0;
        }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int size() { return 6; }
    };

    public EnrichmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.ENRICHMENT_CHAMBER_BLOCK_ENTITY, pos, state, 30_000,
                PESideMode.INPUT, PESideMode.INPUT, PESideMode.ITEM_INPUT,
                PESideMode.ITEM_OUTPUT, PESideMode.ENERGY_ITEM_INPUT,
                PESideMode.ENERGY_ITEM_OUTPUT, PESideMode.DISABLED);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
            EnrichmentChamberBlockEntity chamber) {
        chamber.transferConfiguredItems();
        chamber.sendConfiguredOutput();
        if (!chamber.canWork()) {
            setWorking(world, pos, state, false);
            return;
        }
        ItemStack input = chamber.items.get(0);
        RecipeEntry<EnrichmentRecipe> entry = input.isEmpty() ? null : world.getRecipeManager()
                .getFirstMatch(PhaseTeleportersMod.ENRICHMENT_RECIPE_TYPE,
                        new SingleStackRecipeInput(input), world).orElse(null);
        Identifier recipeId = entry == null ? null : entry.id();
        if (!Objects.equals(chamber.currentRecipeId, recipeId)) {
            chamber.currentRecipeId = recipeId;
            chamber.progress = 0;
            chamber.markDirty();
        }

        ItemStack result = entry == null ? ItemStack.EMPTY
                : entry.value().getResult(world.getRegistryManager());
        ItemStack output = chamber.items.get(1);
        boolean canProcess = !result.isEmpty() && (output.isEmpty()
                || (ItemStack.areItemsAndComponentsEqual(output, result)
                && output.getCount() + result.getCount() <= output.getMaxCount()));
        if (!canProcess) {
            setWorking(world, pos, state, false);
            if (chamber.progress != 0) {
                chamber.progress = 0;
                chamber.markDirty();
            }
            return;
        }
        if (chamber.getStored() < PE_PER_TICK) {
            setWorking(world, pos, state, false);
            return;
        }

        setWorking(world, pos, state, true);
        chamber.extract(PE_PER_TICK, false);
        chamber.progress++;
        if (chamber.progress >= PROCESS_TIME) {
            input.decrement(1);
            if (output.isEmpty()) chamber.items.set(1, result.copy());
            else output.increment(result.getCount());
            chamber.progress = 0;
        }
        chamber.markDirty();
    }

    private static void setWorking(World world, BlockPos pos, BlockState state, boolean working) {
        if (state.get(EnrichmentChamberBlock.LIT) != working)
            world.setBlockState(pos, state.with(EnrichmentChamberBlock.LIT, working),
                    net.minecraft.block.Block.NOTIFY_ALL);
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
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == 0; }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items.clear();
        Inventories.readNbt(nbt, items, lookup);
        progress = Math.clamp(nbt.getInt("Progress"), 0, PROCESS_TIME - 1);
        currentRecipeId = nbt.contains("CurrentRecipe")
                ? Identifier.tryParse(nbt.getString("CurrentRecipe")) : null;
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
        nbt.putInt("Progress", progress);
        if (currentRecipeId != null) nbt.putString("CurrentRecipe", currentRecipeId.toString());
    }

    @Override public Text getDisplayName() {
        return Text.translatable("block.phaseteleporters.enrichment_chamber");
    }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new EnrichmentChamberScreenHandler(syncId, playerInventory, this, properties);
    }
}
