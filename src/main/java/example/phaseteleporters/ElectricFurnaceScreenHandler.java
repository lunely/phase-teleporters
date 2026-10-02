package example.phaseteleporters;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PEBlockEntity;

public final class ElectricFurnaceScreenHandler extends ScreenHandler implements EnergySideScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final PropertyDelegate sideProperties;

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(6));
    }

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, PropertyDelegate properties) {
        super(PhaseTeleportersMod.ELECTRIC_FURNACE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        checkDataCount(properties, 6);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);
        addSlot(new Slot(inventory, 0, 65, 33) {
            @Override public boolean canInsert(ItemStack stack) { return inventory.isValid(0, stack); }
        });
        addSlot(new Slot(inventory, 1, 122, 33) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        addSlot(new example.phaseteleporters.energy.PEEnergyItemSlot(inventory, 2, 8, 62));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addProperties(properties);
        sideProperties = EnergySideConfiguration.properties(
                inventory instanceof PEBlockEntity block ? block : null, playerInventory.player);
        addProperties(sideProperties);
    }

    @Override public example.phaseteleporters.energy.PESideMode getSideMode(net.minecraft.util.math.Direction side) {
        return EnergySideConfiguration.mode(sideProperties, side);
    }

    @Override public net.minecraft.util.math.Direction getSideFacing() {
        return EnergySideConfiguration.facing(sideProperties);
    }

    @Override public example.phaseteleporters.energy.PERedstoneMode getRedstoneMode() {
        return EnergySideConfiguration.redstoneMode(sideProperties);
    }

    @Override public boolean hasRedstoneSignal() {
        return EnergySideConfiguration.hasRedstoneSignal(sideProperties);
    }

    @Override public boolean isPublicAccess() {
        return EnergySideConfiguration.isPublicAccess(sideProperties);
    }

    @Override public boolean isSecurityOwner() {
        return EnergySideConfiguration.isSecurityOwner(sideProperties);
    }

    public int getProgress() { return properties.get(0); }
    public int getProcessTime() { return properties.get(1); }
    public long getEnergy() { return PEPropertyCodec.stored(properties, 2); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(properties, 2); }
    @Override public boolean canUse(PlayerEntity player) { return PESecurity.canUseInventory(player, inventory); }

    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return EnergySideConfiguration.click(player, id,
                    inventory instanceof PEBlockEntity block ? block : null);
        return EnergyConfigurationScreenHandler.open(player, id,
                inventory instanceof PEBlockEntity block ? block : null);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        int machineSlots = 3;
        if (index < machineSlots) {
            if (!insertItem(stack, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof example.phaseteleporters.energy.PEChargeableItem) {
            if (!insertItem(stack, 2, machineSlots, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return original;
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }
}
