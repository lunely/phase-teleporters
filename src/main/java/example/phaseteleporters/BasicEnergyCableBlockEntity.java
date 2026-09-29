package example.phaseteleporters;

import example.phaseteleporters.energy.EnergyCableNetwork;
import example.phaseteleporters.energy.PEStorage;
import example.phaseteleporters.energy.SimplePEStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BasicEnergyCableBlockEntity extends BlockEntity implements PEStorage {
    public static final long CAPACITY_PER_CABLE = 500;
    private final SimplePEStorage localEnergy = new SimplePEStorage(CAPACITY_PER_CABLE);
    private long processedTick = Long.MIN_VALUE;
    private boolean connectionsChecked;

    public BasicEnergyCableBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.BASIC_ENERGY_CABLE_BLOCK_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, BasicEnergyCableBlockEntity cable) {
        if (!cable.connectionsChecked) {
            cable.connectionsChecked = true;
            BasicEnergyCableBlock.refreshConnections(world, pos, state);
        }
        EnergyCableNetwork.distributeStored(world, cable);
    }

    public long localStored() { return localEnergy.getStored(); }
    public void setLocalStored(long amount) {
        if (localEnergy.getStored() == amount) return;
        localEnergy.setStored(amount);
        markDirty();
    }
    public long processedTick() { return processedTick; }
    public void setProcessedTick(long tick) { processedTick = tick; }

    @Override public long getStored() {
        return world == null ? localStored() : EnergyCableNetwork.stored(world, pos);
    }

    @Override public long getCapacity() {
        return world == null ? CAPACITY_PER_CABLE : EnergyCableNetwork.capacity(world, pos);
    }

    @Override public long insert(long amount, boolean simulate) {
        return world == null ? 0 : EnergyCableNetwork.insert(world, pos, amount, simulate);
    }

    @Override public long extract(long amount, boolean simulate) {
        return world == null ? 0 : EnergyCableNetwork.extract(world, pos, amount, simulate);
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        localEnergy.setStored(nbt.getLong("PE"));
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putLong("PE", localStored());
    }
}
