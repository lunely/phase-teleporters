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
    private final SimplePEStorage energy;
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

    @Override public long insert(long amount, boolean simulate) {
        long accepted = energy.insert(amount, simulate);
        if (accepted > 0 && !simulate) markDirty();
        return accepted;
    }

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
