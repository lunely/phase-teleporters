package example.phaseteleports;

import example.phaseteleports.energy.EnergyCableNetwork;
import example.phaseteleports.energy.PEBlockEntity;
import example.phaseteleports.energy.PEPropertyCodec;
import example.phaseteleports.energy.PESideMode;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
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

public final class CoalGeneratorBlockEntity extends PEBlockEntity
        implements Inventory, NamedScreenHandlerFactory {

    public static final long MAX_PE_PER_TICK = 120;
    public static final long MIN_PE_PER_TICK = 30;

    // 12 секунд разогрева.
    private static final int MAX_HEAT = 240;

    private final DefaultedList<ItemStack> items =
            DefaultedList.ofSize(1, ItemStack.EMPTY);

    private int burnTime;
    private int fuelTime;

    private int heat;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return index == 0 ? burnTime
                    : index == 1 ? fuelTime
                    : index >= 2 && index < 6
                    ? PEPropertyCodec.part(CoalGeneratorBlockEntity.this, index - 2)
                    : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                burnTime = value;
            }

            if (index == 1) {
                fuelTime = value;
            }
        }

        @Override
        public int size() {
            return 6;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(
                PhaseTeleportsMod.COAL_GENERATOR_BLOCK_ENTITY,
                pos,
                state,
                PESideMode.OUTPUT,
                PESideMode.OUTPUT,
                PESideMode.DISABLED
        );
    }

    public static void tick(
            World world,
            BlockPos pos,
            BlockState state,
            CoalGeneratorBlockEntity generator
    ) {
        EnergyCableNetwork.distribute(world, pos, generator);

        /*
         * Если сейчас ничего не горит —
         * пробуем взять новое топливо.
         */
        if (generator.burnTime == 0) {
            ItemStack fuel = generator.items.get(0);

            int duration = fuelTime(fuel);

            if (duration == 0) {
                // Топлива нет — генератор постепенно остывает.
                if (generator.heat > 0) {
                    generator.heat--;
                    generator.markDirty();
                }

                return;
            }

            /*
             * Считаем выработку, которая будет после
             * следующего шага нагрева.
             */
            int nextHeat = Math.min(
                    MAX_HEAT,
                    generator.heat + 1
            );

            long nextGeneration = getGenerationForHeat(nextHeat);

            /*
             * Если энергия не помещается —
             * новое топливо пока не сжигаем.
             */
            if (generator.getCapacity() - generator.getStored() < nextGeneration) {
                return;
            }

            generator.burnTime = duration;
            generator.fuelTime = duration;

            net.minecraft.item.Item remainder =
                    fuel.getItem().getRecipeRemainder();

            fuel.decrement(1);

            if (fuel.isEmpty() && remainder != null) {
                generator.items.set(
                        0,
                        new ItemStack(remainder)
                );
            }
        }

        /*
         * Генератор горит.
         *
         * Каждый тик нагреваем его,
         * пока не достигнем максимума.
         */
        if (generator.heat < MAX_HEAT) {
            generator.heat++;
        }

        long currentGeneration =
                getGenerationForHeat(generator.heat);

        /*
         * Если энергия сейчас не помещается,
         * ставим работу на паузу.
         *
         * Топливо не тратится.
         */
        if (generator.getCapacity() - generator.getStored() < currentGeneration) {
            return;
        }

        generator.burnTime--;

        generator.insert(
                currentGeneration,
                false
        );

        generator.markDirty();
    }

    private static long getGenerationForHeat(int heat) {
        int clampedHeat = Math.max(
                0,
                Math.min(heat, MAX_HEAT)
        );

        return MIN_PE_PER_TICK
                + (MAX_PE_PER_TICK - MIN_PE_PER_TICK)
                * clampedHeat
                / MAX_HEAT;
    }

    private static int fuelTime(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        // Уголь = 15 секунд.
        if (stack.isOf(Items.COAL)) {
            return 300;
        }

        // Древесный уголь = 15 секунд.
        if (stack.isOf(Items.CHARCOAL)) {
            return 300;
        }

        // Угольный блок = ровно 9 углей.
        if (stack.isOf(Items.COAL_BLOCK)) {
            return 2700;
        }

        // Всё остальное топливо работает как в ванильной печке.
        return AbstractFurnaceBlockEntity
                .createFuelTimeMap()
                .getOrDefault(stack.getItem(), 0);
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed =
                Inventories.splitStack(items, slot, amount);

        if (!removed.isEmpty()) {
            markDirty();
        }

        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack removed =
                Inventories.removeStack(items, slot);

        if (!removed.isEmpty()) {
            markDirty();
        }

        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        markDirty();
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == 0 && fuelTime(stack) > 0;
    }

    @Override
    protected void readNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.readNbt(nbt, lookup);

        items.clear();

        Inventories.readNbt(
                nbt,
                items,
                lookup
        );

        burnTime = Math.max(
                0,
                nbt.getInt("BurnTime")
        );

        fuelTime = Math.max(
                0,
                nbt.getInt("FuelTime")
        );

        heat = Math.max(
                0,
                Math.min(
                        MAX_HEAT,
                        nbt.getInt("Heat")
                )
        );
    }

    @Override
    protected void writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup lookup
    ) {
        super.writeNbt(nbt, lookup);

        Inventories.writeNbt(
                nbt,
                items,
                lookup
        );

        nbt.putInt(
                "BurnTime",
                burnTime
        );

        nbt.putInt(
                "FuelTime",
                fuelTime
        );

        nbt.putInt(
                "Heat",
                heat
        );
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(
                "block.phaseteleports.coal_generator"
        );
    }

    @Override
    public ScreenHandler createMenu(
            int syncId,
            PlayerInventory inventory,
            PlayerEntity player
    ) {
        return new CoalGeneratorScreenHandler(
                syncId,
                inventory,
                this,
                properties
        );
    }
}