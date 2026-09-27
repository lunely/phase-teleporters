package example.phaseteleports;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import example.phaseteleports.energy.PEPropertyCodec;

public final class InterdimensionalTeleportScreenHandler extends ScreenHandler {
    private final InterdimensionalTeleportBlockEntity teleport;
    private final Inventory anchorInventory;
    private final PropertyDelegate energyProperties;

    public InterdimensionalTeleportScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null, new SimpleInventory(1),
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
        super(PhaseTeleportsMod.INTERDIMENSIONAL_TELEPORT_SCREEN_HANDLER, syncId);
        this.teleport = teleport;
        this.anchorInventory = anchorInventory;
        this.energyProperties = energyProperties;
        checkSize(anchorInventory, 1);
        anchorInventory.onOpen(playerInventory.player);
        addSlot(new Slot(anchorInventory, 0, -23, 132) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.isOf(PhaseTeleportsMod.ANCHOR_UPGRADE);
            }
            @Override public int getMaxItemCount() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 197 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 255));
        addProperties(energyProperties);
    }

    public InterdimensionalTeleportBlockEntity teleport() { return teleport; }
    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        return EnergyConfigurationScreenHandler.open(player, id, teleport);
    }
    public long getEnergy() { return PEPropertyCodec.stored(energyProperties, 0); }
    public long getMaxEnergy() { return PEPropertyCodec.capacity(energyProperties, 0); }

    @Override public boolean canUse(PlayerEntity player) {
        if (teleport == null) return true;
        var pos = teleport.getPos();
        return player.getWorld().getBlockEntity(pos) == teleport
                && player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!insertItem(stack, 1, 37, true)) return ItemStack.EMPTY;
        } else if (stack.isOf(PhaseTeleportsMod.ANCHOR_UPGRADE)) {
            if (!insertItem(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 28) {
            if (!insertItem(stack, 28, 37, false)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 1, 28, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();
        return original;
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        anchorInventory.onClose(player);
    }
}
