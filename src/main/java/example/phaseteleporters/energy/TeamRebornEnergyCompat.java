package example.phaseteleporters.energy;

import example.phaseteleporters.PhaseTeleportersMod;
import example.phaseteleporters.BasicEnergyCableBlockEntity;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/** The API exposes whole units; all internal amounts remain PE. One API unit = 6 PE. */
public final class TeamRebornEnergyCompat {
    public static final long PE_PER_UNIT = 6;
    // Participants belong to buffers, not faces. Two faces or quantum endpoints
    // in the same transaction must never take competing snapshots of one buffer.
    private static final Map<PEStorage, EnergyParticipant> PARTICIPANTS = new WeakHashMap<>();
    private static final ThreadLocal<Set<BasicEnergyCableBlockEntity>> ROUTING =
            ThreadLocal.withInitial(HashSet::new);

    private TeamRebornEnergyCompat() {}

    public static void register() {
        register(PhaseTeleportersMod.BASIC_ENERGY_CABLE_BLOCK_ENTITY);
        register(PhaseTeleportersMod.COAL_GENERATOR_BLOCK_ENTITY);
        register(PhaseTeleportersMod.SOLAR_PANEL_BLOCK_ENTITY);
        register(PhaseTeleportersMod.ENERGY_CUBE_BLOCK_ENTITY);
        register(PhaseTeleportersMod.CREATIVE_ENERGY_CUBE_BLOCK_ENTITY);
        register(PhaseTeleportersMod.INFUSION_STATION_BLOCK_ENTITY);
        register(PhaseTeleportersMod.CRUSHER_BLOCK_ENTITY);
        register(PhaseTeleportersMod.ELECTRIC_FURNACE_BLOCK_ENTITY);
        register(PhaseTeleportersMod.ENRICHMENT_CHAMBER_BLOCK_ENTITY);
        register(PhaseTeleportersMod.TELEPORT_BLOCK_ENTITY);
        register(PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT_BLOCK_ENTITY);
        register(PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY);
        register(PhaseTeleportersMod.EMERGENCY_TELEPORT_BLOCK_ENTITY);
    }

    private static <T extends BlockEntity & PEStorage> void register(BlockEntityType<T> type) {
        EnergyStorage.SIDED.registerForBlockEntity(Bridge::new, type);
    }

    public static boolean connects(World world, BlockPos pos, Direction side) {
        EnergyStorage storage = EnergyStorage.SIDED.find(world, pos, side);
        return storage != null && (storage.supportsInsertion() || storage.supportsExtraction());
    }

    public static boolean accepts(World world, BlockPos pos, Direction side) {
        if (world.isClient) return false;
        EnergyStorage storage = EnergyStorage.SIDED.find(world, pos, side);
        if (storage == null || !storage.supportsInsertion()) return false;
        // A full consumer should not prevent our cables from buffering surplus.
        try (Transaction probe = Transaction.openOuter()) {
            return storage.insert(1, probe) == 1;
        }
    }

    public static long push(World world, BlockPos pos, Direction side, PEStorage source, long limitPE) {
        if (world.isClient || limitPE < PE_PER_UNIT) return 0;
        EnergyStorage receiver = EnergyStorage.SIDED.find(world, pos, side);
        if (receiver == null || !receiver.supportsInsertion()) return 0;
        return EnergyStorageUtil.move(new Bridge(source, null), receiver,
                limitPE / PE_PER_UNIT, null) * PE_PER_UNIT;
    }

    private static synchronized EnergyParticipant participant(PEStorage storage) {
        return PARTICIPANTS.computeIfAbsent(storage, EnergyParticipant::new);
    }

    private static void enlist(PEStorage storage, TransactionContext transaction) {
        for (PEStorage affected : storage.transactionParticipants()) {
            participant(affected).updateSnapshots(transaction);
        }
    }

    private static long insertIntoCable(BasicEnergyCableBlockEntity cable, Direction incomingSide,
            long maxAmount, TransactionContext transaction) {
        World world = cable.getWorld();
        List<BasicEnergyCableBlockEntity> members = EnergyCableNetwork.members(world, cable.getPos());
        Set<BasicEnergyCableBlockEntity> routing = ROUTING.get();
        if (members.stream().anyMatch(routing::contains)) return 0;
        routing.addAll(members);
        try {
            Set<BlockPos> positions = new HashSet<>();
            for (BasicEnergyCableBlockEntity member : members) positions.add(member.getPos());
            BlockPos sender = incomingSide == null ? null : cable.getPos().offset(incomingSide);
            long accepted = 0;
            for (BasicEnergyCableBlockEntity member : members) {
                for (Direction direction : Direction.values()) {
                    long budget = Math.min(remaining(members, world.getTime(), true),
                            remaining(members, world.getTime(), false)) / PE_PER_UNIT;
                    long offer = Math.min(maxAmount - accepted, budget);
                    if (offer == 0) break;
                    BlockPos targetPos = member.getPos().offset(direction);
                    if (positions.contains(targetPos) || targetPos.equals(sender)
                            || !world.isChunkLoaded(targetPos.getX() >> 4, targetPos.getZ() >> 4)) continue;
                    BlockEntity targetBlock = world.getBlockEntity(targetPos);
                    EnergyStorage target = targetBlock instanceof PEStorage pe
                            ? new Bridge(pe, direction.getOpposite())
                            : EnergyStorage.SIDED.find(world, targetPos, direction.getOpposite());
                    if (target == null || !target.supportsInsertion()) continue;
                    long sent = target.insert(offer, transaction);
                    consume(members, world.getTime(), true, sent * PE_PER_UNIT);
                    consume(members, world.getTime(), false, sent * PE_PER_UNIT);
                    accepted += sent;
                }
                if (accepted == maxAmount) break;
            }
            // Consumers get incoming energy first. Only the surplus occupies the
            // small persistent buffer; its size must not cap network throughput.
            long bufferedPE = cable.insert(Bridge.toPE(maxAmount - accepted), true)
                    / PE_PER_UNIT * PE_PER_UNIT;
            accepted += cable.insert(bufferedPE, false) / PE_PER_UNIT;
            return accepted;
        } finally {
            routing.removeAll(members);
            if (routing.isEmpty()) ROUTING.remove();
        }
    }

    private static long remaining(List<BasicEnergyCableBlockEntity> members, long tick, boolean input) {
        long remaining = 0;
        for (BasicEnergyCableBlockEntity cable : members) remaining += cable.remainingTransfer(tick, input);
        return remaining;
    }

    private static void consume(List<BasicEnergyCableBlockEntity> members, long tick,
            boolean input, long amount) {
        for (BasicEnergyCableBlockEntity cable : members) {
            long part = Math.min(amount, cable.remainingTransfer(tick, input));
            cable.consumeTransfer(tick, input, part);
            amount -= part;
            if (amount == 0) return;
        }
    }

    private static final class EnergyParticipant extends SnapshotParticipant<Runnable> {
        private final WeakReference<PEStorage> storage;

        private EnergyParticipant(PEStorage storage) { this.storage = new WeakReference<>(storage); }
        @Override protected Runnable createSnapshot() {
            return Objects.requireNonNull(storage.get()).createEnergySnapshot();
        }
        @Override protected void readSnapshot(Runnable snapshot) { snapshot.run(); }
    }

    static final class Bridge implements EnergyStorage {
        private final PEStorage storage;
        private final Direction side;

        Bridge(PEStorage storage, Direction side) {
            this.storage = storage;
            this.side = side;
        }

        private boolean serverAccess() {
            return !(storage instanceof BlockEntity block)
                    || block.getWorld() != null && !block.getWorld().isClient;
        }

        @Override public boolean supportsInsertion() {
            return !storage.isInfinite() && (!(storage instanceof PEBlockEntity block)
                    || side == null || block.getSideMode(side).allowsInput());
        }
        @Override public boolean supportsExtraction() {
            return !(storage instanceof PEBlockEntity block)
                    || side == null || block.getSideMode(side).allowsOutput();
        }

        @Override public long insert(long maxAmount, TransactionContext transaction) {
            if (maxAmount < 0) throw new IllegalArgumentException("Energy amount must be nonnegative");
            if (maxAmount == 0 || !serverAccess() || !supportsInsertion()) return 0;
            enlist(storage, transaction);
            if (storage instanceof BasicEnergyCableBlockEntity cable)
                return insertIntoCable(cable, side, maxAmount, transaction);
            long allowedPE = storage.insert(toPE(maxAmount), true) / PE_PER_UNIT * PE_PER_UNIT;
            return allowedPE == 0 ? 0 : storage.insert(allowedPE, false) / PE_PER_UNIT;
        }

        @Override public long extract(long maxAmount, TransactionContext transaction) {
            if (maxAmount < 0) throw new IllegalArgumentException("Energy amount must be nonnegative");
            if (maxAmount == 0 || !serverAccess() || !supportsExtraction()) return 0;
            if (storage instanceof PEBlockEntity block && !block.canWork()) return 0;
            enlist(storage, transaction);
            long allowedPE = storage.extract(toPE(maxAmount), true) / PE_PER_UNIT * PE_PER_UNIT;
            return allowedPE == 0 ? 0 : storage.extract(allowedPE, false) / PE_PER_UNIT;
        }

        @Override public long getAmount() { return storage.getStored() / PE_PER_UNIT; }
        @Override public long getCapacity() { return storage.getCapacity() / PE_PER_UNIT; }

        private static long toPE(long amount) {
            return Math.min(amount, Long.MAX_VALUE / PE_PER_UNIT) * PE_PER_UNIT;
        }
    }
}

