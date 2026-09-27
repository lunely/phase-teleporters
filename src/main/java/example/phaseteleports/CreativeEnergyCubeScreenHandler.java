package example.phaseteleports;

import example.phaseteleports.energy.PEChargeableItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public final class CreativeEnergyCubeScreenHandler extends ScreenHandler {
    private final BlockPos pos;
    private final Inventory chargingInventory;

    public CreativeEnergyCubeScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, null, new SimpleInventory(2));
    }

    public CreativeEnergyCubeScreenHandler(int syncId, PlayerInventory inventory,
            CreativeEnergyCubeBlockEntity cube) {
        this(syncId, inventory, cube.getPos(), cube);
    }

    private CreativeEnergyCubeScreenHandler(int syncId, PlayerInventory playerInventory,
            BlockPos pos, Inventory chargingInventory) {
        super(PhaseTeleportsMod.CREATIVE_ENERGY_CUBE_SCREEN_HANDLER, syncId);
        checkSize(chargingInventory, 2);
        this.pos = pos;
        this.chargingInventory = chargingInventory;
        chargingInventory.onOpen(playerInventory.player);
        addSlot(new Slot(chargingInventory, 0, 52, 78) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof PEChargeableItem && chargingInventory.isValid(0, stack);
            }
            @Override public int getMaxItemCount() { return 1; }
        });
        addSlot(new Slot(chargingInventory, 1, 105, 78) {
            @Override public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof PEChargeableItem && chargingInventory.isValid(1, stack);
            }
            @Override public int getMaxItemCount() { return 1; }
        });
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 112 + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 170));
    }

    @Override public boolean canUse(PlayerEntity player) {
        return pos == null || (player.getWorld().getBlockState(pos)
                .isOf(PhaseTeleportsMod.CREATIVE_ENERGY_CUBE)
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0);
    }

    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        return EnergyConfigurationScreenHandler.open(player, id,
                pos != null && player.getWorld().getBlockEntity(pos) instanceof CreativeEnergyCubeBlockEntity cube
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
