package example.phaseteleporters;

import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PEChargeableItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public final class EnergyCubeScreenHandler extends ScreenHandler implements EnergySideScreenHandler {
    private final BlockPos pos;
    private final Inventory chargingInventory;
    private final PropertyDelegate properties;
    private final PropertyDelegate sideProperties;

    public EnergyCubeScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null, new SimpleInventory(2),
                new ArrayPropertyDelegate(PEPropertyCodec.PROPERTY_COUNT));
    }

    public EnergyCubeScreenHandler(int syncId, PlayerInventory inventory,
            EnergyCubeBlockEntity cube, PropertyDelegate properties) {
        this(syncId, inventory, cube.getPos(), cube, properties);
    }

    private EnergyCubeScreenHandler(int syncId, PlayerInventory playerInventory,
            BlockPos pos, Inventory chargingInventory, PropertyDelegate properties) {
        super(PhaseTeleportersMod.ENERGY_CUBE_SCREEN_HANDLER, syncId);
        checkSize(chargingInventory, 2);
        checkDataCount(properties, PEPropertyCodec.PROPERTY_COUNT);
        this.pos = pos;
        this.chargingInventory = chargingInventory;
        this.properties = properties;
        chargingInventory.onOpen(playerInventory.player);
        // Red: PE item -> cube (internal discharge slot 1).
        addSlot(new Slot(chargingInventory, 1, 52, 32) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof PEChargeableItem && chargingInventory.isValid(1, stack);
            }
            @Override public int getMaxItemCount() { return 1; }
        });
        // Blue: cube -> PE item (internal charge slot 0).
        addSlot(new Slot(chargingInventory, 0, 108, 32) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof PEChargeableItem && chargingInventory.isValid(0, stack);
            }
            @Override public int getMaxItemCount() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        addProperties(properties);
        sideProperties = EnergySideConfiguration.properties(
                chargingInventory instanceof EnergyCubeBlockEntity cube ? cube : null, playerInventory.player);
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

    public long getEnergy() { return PEPropertyCodec.stored(properties, 0); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(properties, 0); }

    @Override public boolean canUse(PlayerEntity player) {
        return pos == null || (player.getWorld().getBlockState(pos).isOf(PhaseTeleportersMod.ENERGY_CUBE)
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0
                && player.getWorld().getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube
                && cube.canPlayerUse(player));
    }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return EnergySideConfiguration.click(player, id,
                    pos != null && player.getWorld().getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube
                            ? cube : null);
        return EnergyConfigurationScreenHandler.open(player, id,
                pos != null && player.getWorld().getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube
                        ? cube : null);
    }
    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!insertItem(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof PEChargeableItem) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 29) {
            if (!insertItem(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return original;
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        chargingInventory.onClose(player);
    }
}
