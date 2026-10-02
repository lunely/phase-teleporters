package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** A sided, transactional view of remote fluid stores. No fluid is held in the teleport. */
public final class QuantumFluidStorage implements Storage<FluidVariant> {
    // Ad Astra/CSL reads view 0 even when a port has no extractable fluid.
    // Expose an empty view, not a buffer: it has no capacity and cannot extract.
    private static final StorageView<FluidVariant> EMPTY_VIEW = new StorageView<>() {
        @Override public boolean isResourceBlank() { return true; }
        @Override public FluidVariant getResource() { return FluidVariant.blank(); }
        @Override public long getAmount() { return 0; }
        @Override public long getCapacity() { return 0; }
        @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            checkAmount(resource, maxAmount);
            return 0;
        }
    };
    private static final ThreadLocal<Set<QuantumTeleportBlockEntity>> ROUTING =
            ThreadLocal.withInitial(HashSet::new);
    private final QuantumTeleportBlockEntity teleport;
    private final Direction side;

    private QuantumFluidStorage(QuantumTeleportBlockEntity teleport, Direction side) {
        this.teleport = teleport;
        this.side = side;
    }

    public static void register() {
        FluidStorage.SIDED.registerForBlockEntity(QuantumFluidStorage::new,
                PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY);
    }

    private static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean active(QuantumTeleportBlockEntity endpoint) {
        if (endpoint.isRemoved() || !endpoint.canWork()
                || !(endpoint.getWorld() instanceof ServerWorld world)
                || !loaded(world, endpoint.getPos())
                || world.getBlockEntity(endpoint.getPos()) != endpoint) return false;
        return QuantumFrequencyState.get(world).contains(endpoint.getFrequency(),
                endpoint.isPrivateFrequency(), endpoint.getFrequencyOwner());
    }

    @Override public boolean supportsInsertion() {
        return side != null && active(teleport) && teleport.getSideMode(side).allowsFluidInput();
    }

    @Override public boolean supportsExtraction() {
        return side != null && active(teleport) && teleport.getSideMode(side).allowsFluidOutput();
    }

    private record Port(QuantumTeleportBlockEntity endpoint, Direction side,
            Storage<FluidVariant> storage, boolean input, BlockEntity neighbor, BlockState state) {}

    private boolean valid(Port port) {
        if (!active(teleport) || !active(port.endpoint())) return false;
        ServerWorld world = (ServerWorld) port.endpoint().getWorld();
        BlockPos neighborPos = port.endpoint().getPos().offset(port.side());
        if (!loaded(world, neighborPos) || world.getBlockEntity(neighborPos) != port.neighbor()
                || !world.getBlockState(neighborPos).equals(port.state())) return false;
        if (port.neighbor() instanceof PEBlockEntity machine && (!machine.canWork()
                || (port.input() ? !machine.getSideMode(port.side().getOpposite()).allowsFluidOutput()
                        : !machine.getSideMode(port.side().getOpposite()).allowsFluidInput()))) return false;
        return port.endpoint().matchesFrequency(teleport.getFrequency(),
                        teleport.isPrivateFrequency(), teleport.getFrequencyOwner())
                && (port.input() ? port.endpoint().getSideMode(port.side()).allowsFluidInput()
                        : port.endpoint().getSideMode(port.side()).allowsFluidOutput());
    }

    /** Insertion reaches remote outputs; extraction exposes fluid stores beside remote inputs. */
    private List<Port> ports(boolean input) {
        List<Port> result = new ArrayList<>();
        if (!active(teleport) || side == null) return result;
        ServerWorld world = (ServerWorld) teleport.getWorld();
        BlockPos requester = teleport.getPos().offset(side);
        Set<Storage<FluidVariant>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (QuantumTeleportBlockEntity endpoint : QuantumFrequencyState.get(world).loadedTeleporters()) {
            if (endpoint == teleport || !active(endpoint)
                    || !endpoint.matchesFrequency(teleport.getFrequency(),
                            teleport.isPrivateFrequency(), teleport.getFrequencyOwner())) continue;
            ServerWorld targetWorld = (ServerWorld) endpoint.getWorld();
            for (Direction portSide : Direction.values()) {
                if (input ? !endpoint.getSideMode(portSide).allowsFluidInput()
                        : !endpoint.getSideMode(portSide).allowsFluidOutput()) continue;
                BlockPos targetPos = endpoint.getPos().offset(portSide);
                if (!loaded(targetWorld, targetPos)
                        || targetWorld == world && requester.equals(targetPos)) continue;
                var block = targetWorld.getBlockEntity(targetPos);
                // Configuration slots and other quantum endpoints are never fluid stores.
                if (block instanceof AnchoredTeleportBlockEntity) continue;
                Direction face = portSide.getOpposite();
                if (block instanceof PEBlockEntity machine && (!machine.canWork()
                        || (input ? !machine.getSideMode(face).allowsFluidOutput()
                                : !machine.getSideMode(face).allowsFluidInput()))) continue;
                Storage<FluidVariant> storage = AdAstraFluidPipeStorage.find(targetWorld, targetPos, face);
                if (storage == null || storage instanceof QuantumFluidStorage
                        || (input ? !storage.supportsExtraction() : !storage.supportsInsertion())
                        || !seen.add(storage)) continue;
                result.add(new Port(endpoint, portSide, storage, input, block, targetWorld.getBlockState(targetPos)));
            }
        }
        return result;
    }

    private static void checkAmount(FluidVariant resource, long amount) {
        if (resource == null || resource.isBlank() || amount < 0)
            throw new IllegalArgumentException("Fluid resource must be nonblank and amount nonnegative");
    }

    @Override public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        checkAmount(resource, maxAmount);
        if (maxAmount == 0 || !supportsInsertion()) return 0;
        Set<QuantumTeleportBlockEntity> routing = ROUTING.get();
        if (!routing.add(teleport)) return 0;
        try {
            long accepted = 0;
            for (Port port : ports(false)) {
                if (valid(port)) accepted += port.storage().insert(resource, maxAmount - accepted, transaction);
                if (accepted == maxAmount) break;
            }
            return accepted;
        } finally {
            routing.remove(teleport);
            if (routing.isEmpty()) ROUTING.remove();
        }
    }

    @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        checkAmount(resource, maxAmount);
        if (maxAmount == 0 || !supportsExtraction()) return 0;
        Set<QuantumTeleportBlockEntity> routing = ROUTING.get();
        if (!routing.add(teleport)) return 0;
        try {
            long extracted = 0;
            for (Port port : ports(true)) {
                if (valid(port)) extracted += port.storage().extract(resource, maxAmount - extracted, transaction);
                if (extracted == maxAmount) break;
            }
            return extracted;
        } finally {
            routing.remove(teleport);
            if (routing.isEmpty()) ROUTING.remove();
        }
    }

    @Override public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        if (!supportsExtraction()) return List.of(EMPTY_VIEW).iterator();
        Set<QuantumTeleportBlockEntity> routing = ROUTING.get();
        if (!routing.add(teleport)) return List.of(EMPTY_VIEW).iterator();
        try {
            Set<StorageView<FluidVariant>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Port port : ports(true)) {
                for (StorageView<FluidVariant> view : port.storage()) {
                    if (seen.add(view.getUnderlyingView())) views.add(new RemoteView(port, view));
                }
            }
        } finally {
            routing.remove(teleport);
            if (routing.isEmpty()) ROUTING.remove();
        }
        if (views.isEmpty()) views.add(EMPTY_VIEW);
        return views.iterator();
    }

    private final class RemoteView implements StorageView<FluidVariant> {
        private final Port port;
        private final StorageView<FluidVariant> view;
        private RemoteView(Port port, StorageView<FluidVariant> view) {
            this.port = port;
            this.view = view;
        }
        private boolean accessible() { return supportsExtraction() && valid(port); }
        @Override public boolean isResourceBlank() { return !accessible() || view.isResourceBlank(); }
        @Override public FluidVariant getResource() { return accessible() ? view.getResource() : FluidVariant.blank(); }
        @Override public long getAmount() { return accessible() ? view.getAmount() : 0; }
        @Override public long getCapacity() { return accessible() ? view.getCapacity() : 0; }
        @Override public StorageView<FluidVariant> getUnderlyingView() { return view.getUnderlyingView(); }
        @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            checkAmount(resource, maxAmount);
            if (maxAmount == 0 || !accessible()) return 0;
            Set<QuantumTeleportBlockEntity> routing = ROUTING.get();
            if (!routing.add(teleport)) return 0;
            try {
                return view.extract(resource, maxAmount, transaction);
            } finally {
                routing.remove(teleport);
                if (routing.isEmpty()) ROUTING.remove();
            }
        }
    }
}
