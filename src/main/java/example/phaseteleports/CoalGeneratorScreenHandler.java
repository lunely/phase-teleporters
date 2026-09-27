package example.phaseteleports;

import example.phaseteleports.energy.PEPropertyCodec;
import example.phaseteleports.energy.PEBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class CoalGeneratorScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate properties;

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
                PhaseTeleportsMod.COAL_GENERATOR_SCREEN_HANDLER,
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
                106,
                49
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
        return inventory.canPlayerUse(player);
    }

    @Override
    public boolean onButtonClick(
            PlayerEntity player,
            int id
    ) {
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