package example.phaseteleports;

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
import example.phaseteleports.energy.PEPropertyCodec;
import example.phaseteleports.energy.PEBlockEntity;

public final class ElectricFurnaceScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(2), new ArrayPropertyDelegate(6));
    }

    public ElectricFurnaceScreenHandler(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, PropertyDelegate properties) {
        super(PhaseTeleportsMod.ELECTRIC_FURNACE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 2);
        checkDataCount(properties, 6);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);
        addSlot(new Slot(inventory, 0, 54, 35) {
            @Override public boolean canInsert(ItemStack stack) { return inventory.isValid(0, stack); }
        });
        addSlot(new Slot(inventory, 1, 112, 35) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        addProperties(properties);
    }

    public int getProgress() { return properties.get(0); }
    public int getProcessTime() { return properties.get(1); }
    public long getEnergy() { return PEPropertyCodec.stored(properties, 2); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(properties, 2); }
    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }

    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        return EnergyConfigurationScreenHandler.open(player, id,
                inventory instanceof PEBlockEntity block ? block : null);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!insertItem(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (player.getWorld().getRecipeManager()
                .getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(stack), player.getWorld()).isPresent()) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
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
