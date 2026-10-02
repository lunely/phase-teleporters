package example.phaseteleporters.energy;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.Arrays;
import java.util.UUID;

/** Passive, persistent PE buffer. Machines decide separately if energy affects their work. */
public abstract class PEBlockEntity extends BlockEntity implements PEStorage {
    public static final long DEFAULT_CAPACITY = 100_000;
    /** Matches the coal generator's maximum sustained production. */
    public static final long MAX_INPUT_PER_TICK = 120;
    private final SimplePEStorage energy;
    private long inputTick = Long.MIN_VALUE;
    private long inputThisTick;
    private final PESideMode[] sideModes = new PESideMode[Direction.values().length];
    private final PESideMode defaultSideMode;
    private final PESideMode[] allowedSideModes;
    private PERedstoneMode redstoneMode = PERedstoneMode.IGNORED;
    private UUID owner;
    private boolean publicAccess = true;

    protected PEBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, PESideMode.INPUT, PESideMode.INPUT, PESideMode.DISABLED);
    }

    protected PEBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity) {
        this(type, pos, state, capacity, PESideMode.INPUT, PESideMode.INPUT, PESideMode.DISABLED);
    }

    protected PEBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            PESideMode defaultMode, PESideMode... allowedModes) {
        this(type, pos, state, DEFAULT_CAPACITY, defaultMode, allowedModes);
    }

    protected PEBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long capacity,
            PESideMode defaultMode, PESideMode... allowedModes) {
        super(type, pos, state);
        energy = new SimplePEStorage(capacity);
        defaultSideMode = defaultMode;
        allowedSideModes = allowedModes.clone();
        Arrays.fill(sideModes, defaultMode);
    }

    public PESideMode getSideMode(Direction side) { return sideModes[side.getId()]; }
    public UUID getOwner() { return owner; }
    public boolean isPublicAccess() { return publicAccess; }
    public boolean isOwner(PlayerEntity player) {
        return owner != null && owner.equals(player.getUuid());
    }
    public boolean canPlayerUse(PlayerEntity player) {
        return publicAccess || isOwner(player);
    }
    public void assignOwner(LivingEntity placer) {
        if (placer instanceof PlayerEntity player) {
            owner = player.getUuid();
            markDirty();
        }
    }
    public void claimUnowned(PlayerEntity player) {
        if (owner == null) {
            owner = player.getUuid();
            markDirty();
        }
    }
    public void setPublicAccess(boolean publicAccess) {
        if (this.publicAccess == publicAccess) return;
        this.publicAccess = publicAccess;
        markDirty();
    }
    public PERedstoneMode getRedstoneMode() { return redstoneMode; }
    public void setRedstoneMode(PERedstoneMode mode) {
        if (redstoneMode == mode) return;
        redstoneMode = mode;
        markDirty();
        notifyEnergyNeighbors();
    }
    public boolean hasRedstoneSignal() {
        return world != null && world.isReceivingRedstonePower(pos);
    }
    public boolean canWork() { return redstoneMode.allowsWork(hasRedstoneSignal()); }
    public void setSideMode(Direction side, PESideMode mode) {
        if (!allowsSideMode(mode)) return;
        if (sideModes[side.getId()] == mode) return;
        sideModes[side.getId()] = mode;
        markDirty();
        notifyEnergyNeighbors();
    }

    private void notifyEnergyNeighbors() {
        if (world != null && !world.isClient) world.updateNeighbors(pos, getCachedState().getBlock());
    }

    private boolean allowsSideMode(PESideMode mode) {
        for (PESideMode allowed : allowedSideModes) if (allowed == mode) return true;
        return false;
    }

    public void cycleSideMode(Direction side) {
        for (int i = 0; i < allowedSideModes.length; i++) {
            if (allowedSideModes[i] == getSideMode(side)) {
                setSideMode(side, allowedSideModes[(i + 1) % allowedSideModes.length]);
                return;
            }
        }
        setSideMode(side, defaultSideMode);
    }

    protected void sendConfiguredOutput() {
        if (world == null || world.isClient || getStored() <= 0 || !canWork()) return;
        for (PESideMode mode : sideModes) {
            if (mode.allowsOutput()) {
                EnergyCableNetwork.distribute(world, pos, this);
                return;
            }
        }
    }

    /** Item ports are declared by machines; energy cubes expose no item ports. */
    public int[] itemInputSlots() { return new int[0]; }
    public int[] itemOutputSlots() { return new int[0]; }

    protected void transferConfiguredItems() {
        PEInventoryTransfer.tick(this);
    }

    @Override public long getStored() { return energy.getStored(); }
    @Override public long getCapacity() { return energy.getCapacity(); }

    @Override public Runnable createEnergySnapshot() {
        Runnable savedEnergy = energy.createEnergySnapshot();
        long savedTick = inputTick;
        long savedInput = inputThisTick;
        return () -> {
            savedEnergy.run();
            inputTick = savedTick;
            inputThisTick = savedInput;
            markDirty();
        };
    }

    @Override public long insert(long amount, boolean simulate) {
        long accepted = energy.insert(limitInputThisTick(amount, simulate), simulate);
        if (accepted > 0 && !simulate && world != null && !world.isClient) inputThisTick += accepted;
        if (accepted > 0 && !simulate) markDirty();
        return accepted;
    }

    /** Total input budget across all sides, shared by native and API transfers. */
    protected long getMaxInputPerTick() { return MAX_INPUT_PER_TICK; }

    /** Shared by frequency backed machines so simulated and committed inserts see one limit. */
    protected long limitInputThisTick(long amount, boolean simulate) {
        if (amount <= 0 || world == null || world.isClient) return Math.max(0, amount);
        long tick = world.getTime();
        if (inputTick != tick) {
            inputTick = tick;
            inputThisTick = 0;
        }
        return Math.min(amount, Math.max(0, getMaxInputPerTick() - inputThisTick));
    }

    /** Restore PE carried by a dropped block without applying the live network input limit. */
    public final long restoreStoredEnergy(long amount) { return energy.insert(amount, false); }

    @Override public long extract(long amount, boolean simulate) {
        long extracted = energy.extract(amount, simulate);
        if (extracted > 0 && !simulate) markDirty();
        return extracted;
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        energy.setStored(nbt.getLong("PE"));
        redstoneMode = PERedstoneMode.byId(nbt.getInt("PERedstoneMode"));
        owner = null;
        if (nbt.contains("PEOwner")) {
            try { owner = UUID.fromString(nbt.getString("PEOwner")); }
            catch (IllegalArgumentException ignored) {}
        }
        publicAccess = !nbt.contains("PEPublicAccess") || nbt.getBoolean("PEPublicAccess");
        int[] savedModes = nbt.getIntArray("PESides");
        if (savedModes.length == sideModes.length) {
            for (Direction side : Direction.values()) {
                PESideMode saved = PESideMode.byId(savedModes[side.getId()]);
                sideModes[side.getId()] = allowsSideMode(saved) ? saved : defaultSideMode;
            }
        }
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putLong("PE", energy.getStored());
        nbt.putInt("PERedstoneMode", redstoneMode.ordinal());
        if (owner != null) nbt.putString("PEOwner", owner.toString());
        nbt.putBoolean("PEPublicAccess", publicAccess);
        int[] savedModes = new int[sideModes.length];
        for (Direction side : Direction.values()) savedModes[side.getId()] = getSideMode(side).ordinal();
        nbt.putIntArray("PESides", savedModes);
    }
}
