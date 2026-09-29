package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PESideMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

public final class EnergyConfigurationScreenHandler extends ScreenHandler {
    public static final int OPEN_BUTTON = 99;
    private final PEBlockEntity blockEntity;
    private final PropertyDelegate properties;

    public EnergyConfigurationScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, null, new ArrayPropertyDelegate(7));
    }

    private EnergyConfigurationScreenHandler(int syncId, PEBlockEntity blockEntity,
            PropertyDelegate properties) {
        super(PhaseTeleportersMod.ENERGY_CONFIGURATION_SCREEN_HANDLER, syncId);
        checkDataCount(properties, 7);
        this.blockEntity = blockEntity;
        this.properties = properties;
        addProperties(properties);
    }

    private static PropertyDelegate properties(PEBlockEntity blockEntity) {
        return EnergySideConfiguration.properties(blockEntity);
    }

    public static boolean open(PlayerEntity player, int buttonId, PEBlockEntity blockEntity) {
        if (buttonId != OPEN_BUTTON || !(player instanceof ServerPlayerEntity serverPlayer)
                || blockEntity == null || !canUse(player, blockEntity)) return false;
        serverPlayer.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override public Text getDisplayName() {
                return Text.translatable("gui.phaseteleporters.energy_configuration");
            }
            @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
                return new EnergyConfigurationScreenHandler(syncId, blockEntity, properties(blockEntity));
            }
        });
        return true;
    }

    private static boolean canUse(PlayerEntity player, PEBlockEntity blockEntity) {
        var pos = blockEntity.getPos();
        return player.getWorld().getBlockEntity(pos) == blockEntity
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0
                && blockEntity.canPlayerUse(player);
    }

    public PESideMode getMode(Direction side) {
        return PESideMode.byId(properties.get(side.getId()));
    }

    public Direction getSideFacing() { return EnergySideConfiguration.facing(properties); }

    @Override public boolean canUse(PlayerEntity player) {
        return blockEntity == null || canUse(player, blockEntity);
    }

    @Override public boolean onButtonClick(PlayerEntity player, int buttonId) {
        if (blockEntity == null || buttonId < 0 || buttonId >= 6 || !canUse(player)) return false;
        Direction side = EnergySideConfiguration.sideFor(getSideFacing(), buttonId);
        blockEntity.cycleSideMode(side);
        return true;
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
