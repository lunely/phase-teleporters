package example.phaseteleports;

import example.phaseteleports.energy.PEBlockEntity;
import example.phaseteleports.energy.PESideMode;
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
        this(syncId, null, new ArrayPropertyDelegate(6));
    }

    private EnergyConfigurationScreenHandler(int syncId, PEBlockEntity blockEntity,
            PropertyDelegate properties) {
        super(PhaseTeleportsMod.ENERGY_CONFIGURATION_SCREEN_HANDLER, syncId);
        checkDataCount(properties, 6);
        this.blockEntity = blockEntity;
        this.properties = properties;
        addProperties(properties);
    }

    private static PropertyDelegate properties(PEBlockEntity blockEntity) {
        return new PropertyDelegate() {
            @Override public int get(int index) {
                return blockEntity.getSideMode(Direction.byId(index)).ordinal();
            }
            @Override public void set(int index, int value) {}
            @Override public int size() { return 6; }
        };
    }

    public static boolean open(PlayerEntity player, int buttonId, PEBlockEntity blockEntity) {
        if (buttonId != OPEN_BUTTON || !(player instanceof ServerPlayerEntity serverPlayer)
                || blockEntity == null || !canUse(player, blockEntity)) return false;
        serverPlayer.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override public Text getDisplayName() {
                return Text.translatable("gui.phaseteleports.energy_configuration");
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
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0;
    }

    public PESideMode getMode(Direction side) {
        return PESideMode.byId(properties.get(side.getId()));
    }

    @Override public boolean canUse(PlayerEntity player) {
        return blockEntity == null || canUse(player, blockEntity);
    }

    @Override public boolean onButtonClick(PlayerEntity player, int buttonId) {
        if (blockEntity == null || buttonId < 0 || buttonId >= 6 || !canUse(player)) return false;
        Direction side = Direction.byId(buttonId);
        blockEntity.cycleSideMode(side);
        return true;
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
