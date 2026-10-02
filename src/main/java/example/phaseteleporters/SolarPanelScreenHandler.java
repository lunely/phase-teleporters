package example.phaseteleporters;

import example.phaseteleporters.energy.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Direction;

public final class SolarPanelScreenHandler extends ScreenHandler implements EnergySideScreenHandler {
    private final SolarPanelBlockEntity panel;
    private final Inventory chargingInventory;
    private final PropertyDelegate properties, sides;
    public SolarPanelScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null, new ArrayPropertyDelegate(5));
    }
    public SolarPanelScreenHandler(int syncId, PlayerInventory inventory, SolarPanelBlockEntity panel, PropertyDelegate properties) {
        super(PhaseTeleportersMod.SOLAR_PANEL_SCREEN_HANDLER, syncId);
        this.panel = panel;
        this.chargingInventory = panel == null ? new SimpleInventory(1) : panel;
        checkDataCount(properties, 5);
        this.properties = properties;
        this.sides = EnergySideConfiguration.properties(panel, inventory.player);
        addProperties(properties);
        addProperties(sides);
        addSlot(new Slot(chargingInventory, 0, 8, 62) {
            @Override public boolean canInsert(ItemStack stack) { return stack.getItem() instanceof PEChargeableItem; }
            @Override public int getMaxItemCount() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
    }
    public long energy() { return PEPropertyCodec.stored(properties, 0); }
    public long capacity() { return PEPropertyCodec.capacity(properties, 0); }
    public int generation() { return properties.get(4); }
    @Override public PESideMode getSideMode(Direction side) { return EnergySideConfiguration.mode(sides, side); }
    @Override public Direction getSideFacing() { return EnergySideConfiguration.facing(sides); }
    @Override public PERedstoneMode getRedstoneMode() { return EnergySideConfiguration.redstoneMode(sides); }
    @Override public boolean hasRedstoneSignal() { return EnergySideConfiguration.hasRedstoneSignal(sides); }
    @Override public boolean isPublicAccess() { return EnergySideConfiguration.isPublicAccess(sides); }
    @Override public boolean isSecurityOwner() { return EnergySideConfiguration.isSecurityOwner(sides); }
    @Override public boolean canUse(PlayerEntity player) {
        return panel == null || (player.getWorld().getBlockEntity(panel.getPos()) == panel
                && player.squaredDistanceTo(panel.getPos().toCenterPos()) <= 64 && panel.canPlayerUse(player));
    }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        return canUse(player) && EnergySideConfiguration.click(player, id, panel);
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack(), original = stack.copy();
        if (index == 0) {
            if (!insertItem(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PEChargeableItem) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (!(index < 28 ? insertItem(stack, 28, 37, false) : insertItem(stack, 1, 28, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        return original;
    }
}
