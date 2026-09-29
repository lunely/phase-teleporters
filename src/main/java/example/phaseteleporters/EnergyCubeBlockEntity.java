package example.phaseteleporters;

import example.phaseteleporters.energy.EnergyCableNetwork;
import example.phaseteleporters.energy.PEChargingCubeBlockEntity;
import example.phaseteleporters.energy.PEPropertyCodec;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class EnergyCubeBlockEntity extends PEChargingCubeBlockEntity implements NamedScreenHandlerFactory {
    public static final long CAPACITY = 1_600_000;

    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) { return PEPropertyCodec.part(EnergyCubeBlockEntity.this, index); }
        @Override public void set(int index, int value) {}
        @Override public int size() { return PEPropertyCodec.PROPERTY_COUNT; }
    };

    public EnergyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.ENERGY_CUBE_BLOCK_ENTITY, pos, state, CAPACITY);
    }

    public static void tick(World world, BlockPos pos, BlockState state, EnergyCubeBlockEntity cube) {
        if (!cube.canWork()) return;
        cube.dischargeItem();
        cube.chargeItem();
        if (cube.getStored() > 0) EnergyCableNetwork.distribute(world, pos, cube);
    }

    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.energy_cube"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new EnergyCubeScreenHandler(syncId, inventory, this, properties);
    }
}
