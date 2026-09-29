package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
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

public final class CrusherScreenHandler extends ScreenHandler implements EnergySideScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final PropertyDelegate sideProperties;

    public CrusherScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(2),
                new ArrayPropertyDelegate(6)
        );
    }

    public CrusherScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties
    ) {
        super(PhaseTeleportersMod.CRUSHER_SCREEN_HANDLER, syncId);

        checkSize(inventory, 2);
        checkDataCount(properties, 6);

        this.inventory = inventory;
        this.properties = properties;

        inventory.onOpen(playerInventory.player);

        // Входной слот сверху
        addSlot(new Slot(inventory, 0, 89, 15) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return inventory.isValid(0, stack);
            }
        });

        // Выходной слот снизу
        addSlot(new Slot(inventory, 1, 89, 55) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        // Инвентарь игрока
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInventory,
                        col + row * 9 + 9,
                        8 + col * 18,
                        84 + row * 18
                ));
            }
        }

        // Хотбар
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(
                    playerInventory,
                    col,
                    8 + col * 18,
                    142
            ));
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

    public int getProgress() {
        return properties.get(0);
    }

    public int getProcessTime() {
        return properties.get(1);
    }

    public long getEnergy() {
        return PEPropertyCodec.stored(properties, 2);
    }

    public long getMaxEnergy() {
        return PEPropertyCodec.capacity(properties, 2);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return PESecurity.canUseInventory(player, inventory);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return EnergySideConfiguration.click(player, id,
                    inventory instanceof PEBlockEntity block ? block : null);
        return EnergyConfigurationScreenHandler.open(
                player,
                id,
                inventory instanceof PEBlockEntity block ? block : null
        );
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);

        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index < 2) {
            if (!insertItem(stack, 2, 38, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isOf(Items.OBSIDIAN)) {
            if (!insertItem(stack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }
}
