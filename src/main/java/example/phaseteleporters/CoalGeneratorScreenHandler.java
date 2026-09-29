package example.phaseteleporters;

import example.phaseteleporters.energy.PEPropertyCodec;
import example.phaseteleporters.energy.PEBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class CoalGeneratorScreenHandler extends ScreenHandler implements EnergySideScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final PropertyDelegate sideProperties;

    public CoalGeneratorScreenHandler(
            int syncId,
            PlayerInventory playerInventory
    ) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(1),
                new ArrayPropertyDelegate(6)
        );
    }

    public CoalGeneratorScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties
    ) {
        super(
                PhaseTeleportersMod.COAL_GENERATOR_SCREEN_HANDLER,
                syncId
        );

        checkSize(inventory, 1);
        checkDataCount(properties, 6);

        this.inventory = inventory;
        this.properties = properties;

        inventory.onOpen(playerInventory.player);

        // Топливный слот.
        addSlot(new Slot(
                inventory,
                0,
                91,
                48
        ) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return inventory.isValid(0, stack);
            }
        });

        // Инвентарь игрока.
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

        // Хотбар.
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
                inventory instanceof example.phaseteleporters.energy.PEBlockEntity block ? block : null, playerInventory.player);
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

    public int getBurnTime() {
        return properties.get(0);
    }

    public int getFuelTime() {
        return properties.get(1);
    }

    public long getEnergy() {
        return PEPropertyCodec.stored(
                properties,
                2
        );
    }

    public long getMaxEnergy() {
        return PEPropertyCodec.capacity(
                properties,
                2
        );
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return PESecurity.canUseInventory(player, inventory);
    }

    @Override
    public boolean onButtonClick(
            PlayerEntity player,
            int id
    ) {
        if (id >= EnergySideConfiguration.FIRST_BUTTON && id < EnergySideConfiguration.LAST_BUTTON_EXCLUSIVE)
            return EnergySideConfiguration.click(player, id,
                    inventory instanceof example.phaseteleporters.energy.PEBlockEntity block ? block : null);
        return EnergyConfigurationScreenHandler.open(
                player,
                id,
                inventory instanceof PEBlockEntity block
                        ? block
                        : null
        );
    }

    @Override
    public ItemStack quickMove(
            PlayerEntity player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index == 0) {
            if (!insertItem(
                    stack,
                    1,
                    37,
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else if (
                !inventory.isValid(0, stack)
                        || !insertItem(
                        stack,
                        0,
                        1,
                        false
                )
        ) {
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
