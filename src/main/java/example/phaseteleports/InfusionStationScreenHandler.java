package example.phaseteleports;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import example.phaseteleports.energy.PEPropertyCodec;
import example.phaseteleports.energy.PEBlockEntity;

public final class InfusionStationScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate properties;

    public InfusionStationScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(8));
    }

    public InfusionStationScreenHandler(int syncId, PlayerInventory playerInventory,
                                        Inventory inventory, PropertyDelegate properties) {
        super(PhaseTeleportsMod.INFUSION_STATION_SCREEN_HANDLER, syncId);
        checkSize(inventory, 3);
        checkDataCount(properties, 8);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);
        // Screen slot 0 is infusion (inventory slot 1); screen slot 1 is the main input (inventory slot 0).
        addSlot(new Slot(inventory, 1, 44, 35) {
            @Override public boolean canInsert(ItemStack stack) { return inventory.isValid(1, stack); }
        });
        addSlot(new Slot(inventory, 0, 80, 35) {
            @Override public boolean canInsert(ItemStack stack) { return inventory.isValid(0, stack); }
        });
        addSlot(new Slot(inventory, 2, 116, 35) {
            @Override public boolean canInsert(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 154));
        }
        addProperties(properties);
    }

    public int getProgress() { return properties.get(0); }
    public int getProcessTime() { return properties.get(1); }
    public int getInfusionAmount() { return properties.get(2); }
    public InfusionResource getInfusionResource() { return InfusionResource.byId(properties.get(3)); }
    public long getEnergy() { return PEPropertyCodec.stored(properties, 4); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(properties, 4); }

    @Override public boolean canUse(PlayerEntity player) { return inventory.canPlayerUse(player); }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == EnergyConfigurationScreenHandler.OPEN_BUTTON)
            return EnergyConfigurationScreenHandler.open(player, id,
                    inventory instanceof PEBlockEntity block ? block : null);
        return id == 0 && canUse(player) && inventory instanceof InfusionStationBlockEntity station
                && station.clearInfusion();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 3) {
            if (!insertItem(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (InfusionResource.fromStack(stack) != InfusionResource.NONE) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (stack.isOf(Items.IRON_INGOT) || stack.isOf(PhaseTeleportsMod.INDUSTRIAL_INGOT)
                || stack.isOf(PhaseTeleportsMod.BASIC_ALLOY)
                || stack.isOf(PhaseTeleportsMod.ADVANCED_ALLOY)
                || stack.isOf(PhaseTeleportsMod.OBSIDIAN_DUST)) {
            if (!insertItem(stack, 1, 2, false)) return ItemStack.EMPTY;
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
