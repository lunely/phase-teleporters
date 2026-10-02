package example.phaseteleporters;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PEMachineInventory;
import java.util.List;

public final class InfusionStationBlockEntity extends PEBlockEntity implements PEMachineInventory, NamedScreenHandlerFactory {
    public static final int PROCESS_TIME = 100;
    public static final long PE_PER_TICK = 80;
    public static final int MAX_INFUSION = 1000;
    public record InfusionRecipe(Item input, InfusionResource resource, int units, Item output) {}
    public static final List<InfusionRecipe> RECIPES = List.of(
            new InfusionRecipe(Items.IRON_INGOT, InfusionResource.COAL, 10, PhaseTeleportersMod.STEEL_INGOT),
            new InfusionRecipe(Items.IRON_INGOT, InfusionResource.REDSTONE, 10, PhaseTeleportersMod.BASIC_ALLOY),
            new InfusionRecipe(Items.GOLD_INGOT, InfusionResource.REDSTONE, 20,
                    PhaseTeleportersMod.BASIC_CONTROL_CIRCUIT),
            new InfusionRecipe(PhaseTeleportersMod.BASIC_CONTROL_CIRCUIT,
                    InfusionResource.PURIFIED_OBSIDIAN_DUST, 20, PhaseTeleportersMod.PHASE_CONTROL_CIRCUIT),
            new InfusionRecipe(PhaseTeleportersMod.BASIC_ALLOY, InfusionResource.DIAMOND, 20,
                    PhaseTeleportersMod.ADVANCED_ALLOY),
            new InfusionRecipe(PhaseTeleportersMod.OBSIDIAN_DUST, InfusionResource.DIAMOND, 10,
                    PhaseTeleportersMod.PURIFIED_OBSIDIAN_DUST),
            new InfusionRecipe(Items.LAPIS_LAZULI, InfusionResource.DIAMOND, 100,
                    PhaseTeleportersMod.SOLAR_ELEMENT),
            new InfusionRecipe(PhaseTeleportersMod.ADVANCED_ALLOY, InfusionResource.PURIFIED_OBSIDIAN_DUST, 40,
                    PhaseTeleportersMod.PHASE_ALLOY));
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(4, ItemStack.EMPTY);
    private int progress;
    private InfusionRecipe currentRecipe;
    private int infusionAmount;
    private InfusionResource infusionResource = InfusionResource.NONE;
    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESS_TIME;
                case 2 -> infusionAmount;
                case 3 -> infusionResource.id();
                case 4, 5, 6, 7 -> PEPropertyCodec.part(InfusionStationBlockEntity.this, index - 4);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            if (index == 2) infusionAmount = value;
            if (index == 3) infusionResource = InfusionResource.byId(value);
        }
        @Override public int size() { return 8; }
    };

    public InfusionStationBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.INFUSION_STATION_BLOCK_ENTITY, pos, state, 30_000,
                PESideMode.INPUT, PESideMode.INPUT, PESideMode.ITEM_INPUT,
                PESideMode.ITEM_OUTPUT, PESideMode.ENERGY_ITEM_INPUT,
                PESideMode.ENERGY_ITEM_OUTPUT, PESideMode.DISABLED);
    }

    public static void tick(World world, BlockPos pos, BlockState state, InfusionStationBlockEntity station) {
        example.phaseteleporters.energy.PEEnergyItemTransfer.discharge(station, station.items.get(3));
        station.transferConfiguredItems();
        station.sendConfiguredOutput();
        if (!station.canWork()) return;
        station.loadInfusion();
        InfusionRecipe recipe = station.recipeForInputs();
        if (recipe != station.currentRecipe) {
            station.currentRecipe = recipe;
            station.progress = 0;
        }
        if (recipe == null || !station.canAccept(recipe.output())) {
            if (station.progress != 0) {
                station.progress = 0;
                station.markDirty();
            }
            return;
        }
        if (station.getStored() < PE_PER_TICK) return;
        station.extract(PE_PER_TICK, false);
        station.progress++;
        if (station.progress >= PROCESS_TIME) {
            station.items.get(0).decrement(1);
            station.infusionAmount -= recipe.units();
            if (station.infusionAmount == 0) station.infusionResource = InfusionResource.NONE;
            ItemStack output = station.items.get(2);
            if (output.isEmpty()) station.items.set(2, new ItemStack(recipe.output()));
            else output.increment(1);
            station.progress = 0;
        }
        station.markDirty();
    }

    private void loadInfusion() {
        ItemStack stack = items.get(1);
        InfusionResource resource = InfusionResource.fromStack(stack);
        if (resource == InfusionResource.NONE) return;
        if (infusionAmount > 0 && infusionResource != resource) return;
        int unitsPerStackItem = InfusionResource.unitsForStack(stack);
        boolean loaded = false;
        while (!stack.isEmpty() && unitsPerStackItem <= MAX_INFUSION - infusionAmount) {
            stack.decrement(1);
            infusionAmount += unitsPerStackItem;
            infusionResource = resource;
            loaded = true;
        }
        if (loaded) markDirty();
    }

    private InfusionRecipe recipeForInputs() {
        for (InfusionRecipe recipe : RECIPES)
            if (infusionResource == recipe.resource() && infusionAmount >= recipe.units()
                    && items.get(0).isOf(recipe.input())) return recipe;
        return null;
    }

    public boolean clearInfusion() {
        if (progress != 0 || infusionAmount == 0) return false;
        infusionAmount = 0;
        infusionResource = InfusionResource.NONE;
        currentRecipe = null;
        markDirty();
        return true;
    }

    public void writeStoredInfusion(NbtCompound data) {
        if (infusionAmount <= 0 || infusionResource == InfusionResource.NONE) return;
        data.putInt("InfusionAmount", infusionAmount);
        data.putInt("InfusionResource", infusionResource.id());
    }

    public void readStoredInfusion(NbtCompound data) {
        infusionResource = InfusionResource.byId(data.getInt("InfusionResource"));
        infusionAmount = infusionResource == InfusionResource.NONE ? 0
                : Math.clamp(data.getInt("InfusionAmount"), 0, MAX_INFUSION);
        if (infusionAmount == 0) infusionResource = InfusionResource.NONE;
        markDirty();
    }

    private boolean canAccept(Item result) {
        ItemStack output = items.get(2);
        return output.isEmpty() || (output.isOf(result) && output.getCount() < output.getMaxCount());
    }

    @Override public int size() { return items.size(); }
    @Override public int[] itemInputSlots() { return new int[] {0, 1}; }
    @Override public int[] itemOutputSlots() { return new int[] {2}; }
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
    @Override public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        markDirty();
    }
    @Override public void clear() {
        items.clear();
        markDirty();
    }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> RECIPES.stream().anyMatch(recipe -> stack.isOf(recipe.input()));
            case 1 -> InfusionResource.fromStack(stack) != InfusionResource.NONE;
            case 3 -> stack.getItem() instanceof example.phaseteleporters.energy.PEChargeableItem;
            default -> false;
        };
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items.clear();
        Inventories.readNbt(nbt, items, lookup);
        progress = nbt.getInt("Progress");
        infusionResource = InfusionResource.byId(nbt.getInt("InfusionResource"));
        infusionAmount = infusionResource == InfusionResource.NONE ? 0
                : Math.clamp(nbt.getInt("InfusionAmount"), 0, MAX_INFUSION);
        if (infusionAmount == 0) infusionResource = InfusionResource.NONE;
        currentRecipe = recipeForInputs();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
        nbt.putInt("Progress", progress);
        nbt.putInt("InfusionAmount", infusionAmount);
        nbt.putInt("InfusionResource", infusionResource.id());
    }

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.infusion_station"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new InfusionStationScreenHandler(syncId, playerInventory, this, properties);
    }
}
