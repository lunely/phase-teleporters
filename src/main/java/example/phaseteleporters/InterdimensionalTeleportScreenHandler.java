package example.phaseteleporters;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.slot.Slot;
import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PEChargeableItem;

public final class InterdimensionalTeleportScreenHandler extends ScreenHandler implements EnergySideScreenHandler {
    private final InterdimensionalTeleportBlockEntity teleport;
    private final Inventory anchorInventory;
    private final PropertyDelegate energyProperties;
    private final PropertyDelegate sideProperties;
    private final PropertyDelegate statusProperties;

    public InterdimensionalTeleportScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null, new SimpleInventory(2),
                new ArrayPropertyDelegate(PEPropertyCodec.PROPERTY_COUNT));
    }

    public InterdimensionalTeleportScreenHandler(int syncId, PlayerInventory inventory,
            InterdimensionalTeleportBlockEntity teleport) {
        this(syncId, inventory, teleport, teleport, new PropertyDelegate() {
            @Override public int get(int index) { return PEPropertyCodec.part(teleport, index); }
            @Override public void set(int index, int value) {}
            @Override public int size() { return PEPropertyCodec.PROPERTY_COUNT; }
        });
    }

    private InterdimensionalTeleportScreenHandler(int syncId, PlayerInventory playerInventory,
            InterdimensionalTeleportBlockEntity teleport, Inventory anchorInventory,
            PropertyDelegate energyProperties) {
        super(PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT_SCREEN_HANDLER, syncId);
        this.teleport = teleport;
        this.anchorInventory = anchorInventory;
        this.energyProperties = energyProperties;
        checkSize(anchorInventory, 2);
        anchorInventory.onOpen(playerInventory.player);
        addSlot(new Slot(anchorInventory, 0, 8, 132) {
            @Override public boolean canInsert(ItemStack stack) {
                return anchorInventory.isValid(0, stack);
            }
        });
        addSlot(new Slot(anchorInventory, 1, 152, 79) {
            @Override public boolean canInsert(ItemStack stack) {
                return anchorInventory.isValid(1, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 155 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 213));
        }
        addProperties(energyProperties);
        sideProperties = EnergySideConfiguration.properties(teleport, playerInventory.player);
        addProperties(sideProperties);
        statusProperties = teleport == null ? new ArrayPropertyDelegate(1) : new PropertyDelegate() {
            @Override public int get(int index) { return teleport.getPortalStatus(); }
            @Override public void set(int index, int value) {}
            @Override public int size() { return 1; }
        };
        addProperties(statusProperties);
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

    public InterdimensionalTeleportBlockEntity teleport() { return teleport; }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return EnergySideConfiguration.click(player, id, teleport);
        return EnergyConfigurationScreenHandler.open(player, id, teleport);
    }
    public long getEnergy() { return PEPropertyCodec.stored(energyProperties, 0); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(energyProperties, 0); }
    public int getPortalStatus() { return statusProperties.get(0); }

    @Override public boolean canUse(PlayerEntity player) {
        if (teleport == null) return true;
        var pos = teleport.getPos();
        return player.getWorld().getBlockEntity(pos) == teleport
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64
                && teleport.canPlayerUse(player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index < 2) {
            if (!insertItem(stack, 2, 38, true)) return ItemStack.EMPTY;
        } else if (stack.isOf(PhaseTeleportersMod.ANCHOR_UPGRADE)
                && insertItem(stack, 0, 1, false)) {
            // Upgrade inserted into the teleporter.
        } else if (stack.getItem() instanceof PEChargeableItem
                && insertItem(stack, 1, 2, false)) {
            // Energy item inserted into the teleporter.
        } else if (index < 29) {
            if (!insertItem(stack, 29, 38, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 2, 29, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return original;
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        anchorInventory.onClose(player);
    }
}
