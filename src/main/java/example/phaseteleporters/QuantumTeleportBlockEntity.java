package example.phaseteleporters;

import java.util.Objects;
import java.util.UUID;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PEStorage;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Frequency energy buffer and direct item routing; inventory holds configuration items only. */
public final class QuantumTeleportBlockEntity extends AnchoredTeleportBlockEntity implements NamedScreenHandlerFactory, SidedInventory {
    public static final long CAPACITY = 10_000_000;
    public static final long MAX_INPUT_PER_TICK = 10_000;
    private String frequency = "";
    private boolean privateFrequency;
    private UUID frequencyOwner;
    private long legacyEnergy;
    private long inputTick = Long.MIN_VALUE;
    private long inputThisTick;

    public QuantumTeleportBlockEntity(BlockPos pos, BlockState state) {
        super(PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY, pos, state, CAPACITY,
                PESideMode.ALL_INPUT, PESideMode.INPUT, PESideMode.OUTPUT, PESideMode.INPUT_OUTPUT,
                PESideMode.ITEM_INPUT, PESideMode.ITEM_OUTPUT, PESideMode.FLUID_INPUT, PESideMode.FLUID_OUTPUT,
                PESideMode.ALL_INPUT, PESideMode.ALL_OUTPUT, PESideMode.DISABLED);
    }
    public String getFrequency() { return frequency; }
    public boolean isPrivateFrequency() { return privateFrequency; }
    public UUID getFrequencyOwner() { return frequencyOwner; }

    private PEStorage frequencyEnergy() {
        if (frequency.isEmpty() || !(world instanceof ServerWorld serverWorld)) return null;
        return QuantumFrequencyState.get(serverWorld).energy(frequency, privateFrequency, frequencyOwner);
    }

    @Override public long getStored() {
        PEStorage storage = frequencyEnergy();
        return storage == null ? 0 : storage.getStored();
    }
    @Override public long getCapacity() { return CAPACITY; }
    @Override public Iterable<? extends PEStorage> transactionParticipants() {
        PEStorage shared = frequencyEnergy();
        return shared == null ? java.util.List.of(this) : java.util.List.of(this, shared);
    }
    @Override public Runnable createEnergySnapshot() {
        // The frequency buffer has its own participant, shared by every endpoint.
        long savedTick = inputTick;
        long savedInput = inputThisTick;
        return () -> {
            inputTick = savedTick;
            inputThisTick = savedInput;
        };
    }
    @Override public long insert(long amount, boolean simulate) {
        PEStorage storage = frequencyEnergy();
        long accepted = storage == null ? 0 : storage.insert(limitInputThisTick(amount, simulate), simulate);
        if (accepted > 0 && !simulate && world != null && !world.isClient) inputThisTick += accepted;
        return accepted;
    }
    @Override protected long limitInputThisTick(long amount, boolean simulate) {
        if (amount <= 0 || world == null || world.isClient) return Math.max(0, amount);
        long tick = world.getTime();
        if (inputTick != tick) {
            inputTick = tick;
            inputThisTick = 0;
        }
        return Math.min(amount, Math.max(0, MAX_INPUT_PER_TICK - inputThisTick));
    }
    @Override public long extract(long amount, boolean simulate) {
        PEStorage storage = frequencyEnergy();
        return storage == null ? 0 : storage.extract(amount, simulate);
    }
    @Override public long transferTo(PEStorage receiver, long limit) {
        // Two ports on the same frequency are already the same buffer.
        if (receiver instanceof QuantumTeleportBlockEntity other
                && frequencyEnergy() != null && frequencyEnergy() == other.frequencyEnergy()) return 0;
        return super.transferTo(receiver, limit);
    }
    public boolean matchesFrequency(String name, boolean privateType, UUID owner) {
        return frequency.equals(name) && privateFrequency == privateType
                && (!privateType || Objects.equals(frequencyOwner, owner));
    }
    public void setFrequency(String name, boolean privateType, UUID owner) {
        frequency = QuantumFrequencyState.normalize(name);
        privateFrequency = privateType;
        frequencyOwner = privateType ? owner : null;
        markDirty();
        if (world != null && !world.isClient) world.updateNeighbors(pos, getCachedState().getBlock());
    }
    public void removeFrequency() { setFrequency("", false, null); }
    public void writeStoredFrequency(NbtCompound data) {
        if (frequencyEnergy() == null) return;
        data.putString("QuantumFrequency", frequency);
        data.putBoolean("QuantumFrequencyPrivate", privateFrequency);
        if (frequencyOwner != null) data.putUuid("QuantumFrequencyOwner", frequencyOwner);
    }
    public void readStoredFrequency(NbtCompound data) {
        String name = QuantumFrequencyState.normalize(data.getString("QuantumFrequency"));
        boolean privateType = data.getBoolean("QuantumFrequencyPrivate");
        UUID owner = data.containsUuid("QuantumFrequencyOwner") ? data.getUuid("QuantumFrequencyOwner") : null;
        if (!name.isEmpty() && (!privateType || owner != null)
                && world instanceof ServerWorld serverWorld
                && QuantumFrequencyState.get(serverWorld).contains(name, privateType, owner)) {
            setFrequency(name, privateType, owner);
        }
    }
    public int getPortalStatus() {
        if (!canWork()) return PortalStatus.REDSTONE;
        return frequencyEnergy() == null ? PortalStatus.NO_FREQUENCY : PortalStatus.ACTIVE;
    }
    // Batteries and upgrades are GUI slots, never a buffer for transported items.
    @Override public int[] getAvailableSlots(Direction side) { return new int[0]; }
    @Override public boolean canInsert(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction side) { return false; }

    @Override public void markRemoved() {
        if (world instanceof ServerWorld serverWorld) QuantumFrequencyState.get(serverWorld).unregister(this);
        super.markRemoved();
    }
    public static void tick(World world, BlockPos pos, BlockState state, QuantumTeleportBlockEntity teleport) {
        if (world instanceof ServerWorld serverWorld) QuantumFrequencyState.get(serverWorld).register(teleport);
        if (teleport.legacyEnergy > 0) {
            PEStorage storage = teleport.frequencyEnergy();
            if (storage != null) {
                long restored = teleport.insert(teleport.legacyEnergy, false);
                teleport.legacyEnergy -= restored;
                teleport.markDirty();
            }
        }
        if (world.getTime() % 20 == 0) {
            teleport.syncAnchor();
            if (!teleport.frequency.isEmpty() && world instanceof ServerWorld serverWorld
                    && !QuantumFrequencyState.get(serverWorld).contains(teleport.frequency,
                            teleport.privateFrequency, teleport.frequencyOwner)) teleport.removeFrequency();
        }
        teleport.dischargeEnergyItem();
        teleport.sendConfiguredOutput();
        QuantumItemTransfer.tick(teleport);
        QuantumFluidTransfer.tick(teleport);
    }
    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        // Map the old combined modes to their universal equivalents without
        // changing separately configured energy or item sides on reload.
        int[] savedModes = nbt.getIntArray("PESides");
        if (savedModes.length == Direction.values().length) {
            for (Direction side : Direction.values()) {
                PESideMode mode = PESideMode.byId(savedModes[side.getId()]);
                if (mode == PESideMode.ENERGY_ITEM_OUTPUT) setSideMode(side, PESideMode.ALL_OUTPUT);
                else if (mode == PESideMode.ENERGY_ITEM_INPUT) setSideMode(side, PESideMode.ALL_INPUT);
            }
        }
        frequency = QuantumFrequencyState.normalize(nbt.getString("QuantumFrequency"));
        privateFrequency = nbt.getBoolean("QuantumFrequencyPrivate");
        frequencyOwner = nbt.containsUuid("QuantumFrequencyOwner") ? nbt.getUuid("QuantumFrequencyOwner") : null;
        if (privateFrequency && frequencyOwner == null) removeFrequency();
        // Migrate old per-block PE into its selected frequency once, on the first server tick.
        legacyEnergy = frequency.isEmpty() ? 0 : Math.clamp(nbt.getLong(
                nbt.contains("QuantumLegacyPE") ? "QuantumLegacyPE" : "PE"), 0L, CAPACITY);
    }
    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.remove("PE");
        if (legacyEnergy > 0) nbt.putLong("QuantumLegacyPE", legacyEnergy);
        else nbt.remove("QuantumLegacyPE");
        nbt.putString("QuantumFrequency", frequency);
        nbt.putBoolean("QuantumFrequencyPrivate", privateFrequency);
        if (frequencyOwner != null) nbt.putUuid("QuantumFrequencyOwner", frequencyOwner);
    }
    @Override public Text getDisplayName() { return Text.translatable("block.phaseteleporters.quantum_teleporter"); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new QuantumTeleportScreenHandler(syncId, inventory, this);
    }
}
