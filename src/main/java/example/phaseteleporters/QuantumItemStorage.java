package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** A sided, transactional view of remote inventories. No items are stored in the teleport. */
public final class QuantumItemStorage implements Storage<ItemVariant> {
    private static final ThreadLocal<Set<QuantumTeleportBlockEntity>> ROUTING =
            ThreadLocal.withInitial(HashSet::new);
    private final QuantumTeleportBlockEntity teleport;
    private final Direction side;

    private QuantumItemStorage(QuantumTeleportBlockEntity teleport, Direction side) {
        this.teleport = teleport;
        this.side = side;
    }

    public static void register() {
        ItemStorage.SIDED.registerForBlockEntity(QuantumItemStorage::new,
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
        return side != null && active(teleport) && teleport.getSideMode(side).allowsItemInput();
    }

    @Override public boolean supportsExtraction() {
        return side != null && active(teleport) && teleport.getSideMode(side).allowsItemOutput();
    }

    private record Port(QuantumTeleportBlockEntity endpoint, Direction side,
            Storage<ItemVariant> storage, boolean input, BlockEntity neighbor, BlockState state) {}

    private boolean valid(Port port) {
        if (!active(teleport) || !active(port.endpoint())) return false;
        ServerWorld world = (ServerWorld) port.endpoint().getWorld();
        BlockPos neighborPos = port.endpoint().getPos().offset(port.side());
        if (!loaded(world, neighborPos) || world.getBlockEntity(neighborPos) != port.neighbor()
                || !world.getBlockState(neighborPos).equals(port.state())) return false;
        if (port.neighbor() instanceof PEBlockEntity machine && (!machine.canWork()
                || (port.input() ? !machine.getSideMode(port.side().getOpposite()).allowsItemOutput()
                        : !machine.getSideMode(port.side().getOpposite()).allowsItemInput()))) return false;
        return port.endpoint().matchesFrequency(teleport.getFrequency(),
                        teleport.isPrivateFrequency(), teleport.getFrequencyOwner())
                && (port.input() ? port.endpoint().getSideMode(port.side()).allowsItemInput()
                        : port.endpoint().getSideMode(port.side()).allowsItemOutput());
    }

    /** Insertion reaches remote outputs; extraction exposes inventories beside remote inputs. */
    private List<Port> ports(boolean input) {
        List<Port> result = new ArrayList<>();
        if (!active(teleport) || side == null) return result;
        ServerWorld world = (ServerWorld) teleport.getWorld();
        BlockPos requester = teleport.getPos().offset(side);
        Set<Storage<ItemVariant>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (QuantumTeleportBlockEntity endpoint : QuantumFrequencyState.get(world).loadedTeleporters()) {
            if (endpoint == teleport || !active(endpoint)
                    || !endpoint.matchesFrequency(teleport.getFrequency(),
                            teleport.isPrivateFrequency(), teleport.getFrequencyOwner())) continue;
            ServerWorld targetWorld = (ServerWorld) endpoint.getWorld();
            for (Direction portSide : Direction.values()) {
                if (input ? !endpoint.getSideMode(portSide).allowsItemInput()
                        : !endpoint.getSideMode(portSide).allowsItemOutput()) continue;
                BlockPos targetPos = endpoint.getPos().offset(portSide);
                if (!loaded(targetWorld, targetPos)
                        || targetWorld == world && sameInventory(world, requester, targetPos)) continue;
                var block = targetWorld.getBlockEntity(targetPos);
                // Configuration slots and other quantum endpoints are never cargo inventories.
                if (block instanceof AnchoredTeleportBlockEntity) continue;
                Direction face = portSide.getOpposite();
                if (block instanceof PEBlockEntity machine && (!machine.canWork()
                        || (input ? !machine.getSideMode(face).allowsItemOutput()
                                : !machine.getSideMode(face).allowsItemInput()))) continue;
                Storage<ItemVariant> storage = ItemStorage.SIDED.find(targetWorld, targetPos, face);
                if (storage == null || storage instanceof QuantumItemStorage
                        || (input ? !storage.supportsExtraction() : !storage.supportsInsertion())
                        || !seen.add(storage)) continue;
                result.add(new Port(endpoint, portSide, storage, input, block, targetWorld.getBlockState(targetPos)));
            }
        }
        return result;
    }

    private static boolean sameInventory(ServerWorld world, BlockPos first, BlockPos second) {
        if (first.equals(second)) return true;
        if (!loaded(world, first)) return false;
        var state = world.getBlockState(first);
        if (!(state.getBlock() instanceof ChestBlock)) return false;
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE) return false;
        Direction facing = state.get(ChestBlock.FACING);
        Direction other = type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
        return first.offset(other).equals(second);
    }

    private static void checkAmount(ItemVariant resource, long amount) {
        if (resource == null || resource.isBlank() || amount < 0)
            throw new IllegalArgumentException("Item resource must be nonblank and amount nonnegative");
    }

    @Override public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
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

    @Override public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
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

    @Override public Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        if (!supportsExtraction()) return views.iterator();
        Set<QuantumTeleportBlockEntity> routing = ROUTING.get();
        if (!routing.add(teleport)) return views.iterator();
        try {
            Set<StorageView<ItemVariant>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Port port : ports(true)) {
                for (StorageView<ItemVariant> view : port.storage()) {
                    if (seen.add(view.getUnderlyingView())) views.add(new RemoteView(port, view));
                }
            }
        } finally {
            routing.remove(teleport);
            if (routing.isEmpty()) ROUTING.remove();
        }
        return views.iterator();
    }

    private final class RemoteView implements StorageView<ItemVariant> {
        private final Port port;
        private final StorageView<ItemVariant> view;
        private RemoteView(Port port, StorageView<ItemVariant> view) {
            this.port = port;
            this.view = view;
        }
        private boolean accessible() { return supportsExtraction() && valid(port); }
        @Override public boolean isResourceBlank() { return !accessible() || view.isResourceBlank(); }
        @Override public ItemVariant getResource() { return accessible() ? view.getResource() : ItemVariant.blank(); }
        @Override public long getAmount() { return accessible() ? view.getAmount() : 0; }
        @Override public long getCapacity() { return accessible() ? view.getCapacity() : 0; }
        @Override public StorageView<ItemVariant> getUnderlyingView() { return view.getUnderlyingView(); }
        @Override public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
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
