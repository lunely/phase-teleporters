package example.phaseteleporters;

import example.phaseteleporters.energy.PEChargingCubeBlockEntity;
import example.phaseteleporters.energy.EnergyCableNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CreativeEnergyCubeBlockEntity extends PEChargingCubeBlockEntity
        implements NamedScreenHandlerFactory {
    public CreativeEnergyCubeBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.CREATIVE_ENERGY_CUBE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, CreativeEnergyCubeBlockEntity cube) {
        if (!cube.canWork()) return;
        cube.chargeItem();
        EnergyCableNetwork.distribute(world, pos, cube);
    }

    @Override public long getStored() { return Long.MAX_VALUE; }
    @Override public long getCapacity() { return Long.MAX_VALUE; }
    @Override public boolean isInfinite() { return true; }
    @Override public long insert(long amount, boolean simulate) { return 0; }
    @Override public long extract(long amount, boolean simulate) { return Math.max(0L, amount); }

    @Override public Text getDisplayName() {
        return Text.translatable("block.phaseteleporters.creative_energy_cube");
    }

    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new CreativeEnergyCubeScreenHandler(syncId, inventory, this);
    }
}
