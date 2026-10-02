package example.phaseteleporters;

import java.util.ArrayDeque;
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
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Optional, bufferless access to tanks behind Ad Astra pipes, used only by quantum teleports. */
final class AdAstraFluidPipeStorage implements Storage<FluidVariant> {
    private static final int MAX_PIPES = 4096;
    private final ServerWorld world;
    private final BlockPos origin;
    private final Direction face;

    private AdAstraFluidPipeStorage(ServerWorld world, BlockPos origin, Direction face) {
        this.world = world;
        this.origin = origin.toImmutable();
        this.face = face;
    }

    static Storage<FluidVariant> find(ServerWorld world, BlockPos pos, Direction face) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, pos, face);
        if (storage != null) return storage;
        return pipeRate(world.getBlockState(pos)) > 0 && face != null
                ? new AdAstraFluidPipeStorage(world, pos, face) : null;
    }

    private static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long pipeRate(BlockState state) {
        var id = Registries.BLOCK.getId(state.getBlock());
        if (!id.getNamespace().equals("ad_astra")) return 0;
        if (!Set.of("desh_fluid_pipe", "ostrum_fluid_pipe", "fluid_pipe_duct").contains(id.getPath())) return 0;
        // Public Ad Astra method; no class loading or dependency when the mod is absent.
        try {
            return Math.max(0, (Long) state.getBlock().getClass().getMethod("transferRate").invoke(state.getBlock()));
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return 0;
        }
    }

    private static String connection(BlockState state, Direction side) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("connected_" + side.asString()))
                return connectionName(state, property);
        }
        return "none";
    }

    private static <T extends Comparable<T>> String connectionName(BlockState state, Property<T> property) {
        return property.name(state.get(property));
    }

    private boolean open(boolean input) {
        if (!loaded(world, origin) || pipeRate(world.getBlockState(origin)) == 0) return false;
        String mode = connection(world.getBlockState(origin), face);
        // At the teleport end, extract means pull FROM the teleport; insert means push INTO it.
        return mode.equals("normal") || mode.equals(input ? "extract" : "insert");
    }

    @Override public boolean supportsInsertion() { return open(true); }
    @Override public boolean supportsExtraction() { return open(false); }

    private record Node(BlockPos pos, long rate) {}
    private record Tank(BlockPos pos, Direction face, Storage<FluidVariant> storage, long rate,
            BlockEntity entity, BlockState state) {}

    private List<Tank> tanks(boolean input) {
        List<Tank> tanks = new ArrayList<>();
        if (!open(input)) return tanks;
        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<String> seenFaces = new HashSet<>();
        Set<Storage<FluidVariant>> seenStorages = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        pending.add(new Node(origin, pipeRate(world.getBlockState(origin))));
        visited.add(origin);
        BlockPos requester = origin.offset(face);
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            BlockState state = world.getBlockState(node.pos());
            for (Direction side : Direction.values()) {
                String mode = connection(state, side);
                if (mode.equals("none")) continue;
                BlockPos next = node.pos().offset(side);
                if (!loaded(world, next)) continue;
                BlockState nextState = world.getBlockState(next);
                long rate = pipeRate(nextState);
                if (rate > 0) {
                    if (!connection(nextState, side.getOpposite()).equals("none") && visited.add(next)) {
                        if (visited.size() > MAX_PIPES) return List.of();
                        pending.addLast(new Node(next, Math.min(node.rate(), rate)));
                    }
                    continue;
                }
                if (next.equals(requester) || world.getBlockEntity(next) instanceof AnchoredTeleportBlockEntity
                        || !(mode.equals("normal") || mode.equals(input ? "insert" : "extract"))) continue;
                Direction tankFace = side.getOpposite();
                String key = next.asLong() + ":" + tankFace.getId();
                if (!seenFaces.add(key)) continue;
                Storage<FluidVariant> storage = FluidStorage.SIDED.find(world, next, tankFace);
                if (storage == null || storage instanceof QuantumFluidStorage
                        || (input ? !storage.supportsInsertion() : !storage.supportsExtraction())
                        || !seenStorages.add(storage)) continue;
                tanks.add(new Tank(next, tankFace, storage, node.rate(), world.getBlockEntity(next), nextState));
            }
        }
        return tanks;
    }

    private static void check(FluidVariant resource, long amount) {
        if (resource == null || resource.isBlank() || amount < 0) throw new IllegalArgumentException("Invalid fluid transfer");
    }

    @Override public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        check(resource, maxAmount);
        if (!supportsInsertion() || maxAmount == 0) return 0;
        long limit = Math.min(maxAmount, pipeRate(world.getBlockState(origin)));
        long moved = 0;
        for (Tank tank : tanks(true)) {
            moved += tank.storage().insert(resource, Math.min(limit - moved, tank.rate()), transaction);
            if (moved == limit) break;
        }
        return moved;
    }

    @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        check(resource, maxAmount);
        if (!supportsExtraction() || maxAmount == 0) return 0;
        long limit = Math.min(maxAmount, pipeRate(world.getBlockState(origin)));
        long moved = 0;
        for (Tank tank : tanks(false)) {
            moved += tank.storage().extract(resource, Math.min(limit - moved, tank.rate()), transaction);
            if (moved == limit) break;
        }
        return moved;
    }

    @Override public Iterator<StorageView<FluidVariant>> iterator() {
        List<StorageView<FluidVariant>> views = new ArrayList<>();
        Set<StorageView<FluidVariant>> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Tank tank : tanks(false)) {
            for (StorageView<FluidVariant> view : tank.storage()) {
                if (seen.add(view.getUnderlyingView())) views.add(new PipeView(tank, view));
            }
        }
        return views.iterator();
    }

    private final class PipeView implements StorageView<FluidVariant> {
        private final Tank tank;
        private final StorageView<FluidVariant> view;
        private PipeView(Tank tank, StorageView<FluidVariant> view) { this.tank = tank; this.view = view; }
        private boolean accessible() {
            // Recheck the network and wrench settings before using a retained view.
            if (!loaded(world, tank.pos()) || world.getBlockEntity(tank.pos()) != tank.entity()
                    || !world.getBlockState(tank.pos()).equals(tank.state())) return false;
            return tanks(false).stream().anyMatch(current -> current.pos().equals(tank.pos()) && current.face() == tank.face());
        }
        @Override public boolean isResourceBlank() { return !accessible() || view.isResourceBlank(); }
        @Override public FluidVariant getResource() { return accessible() ? view.getResource() : FluidVariant.blank(); }
        @Override public long getAmount() { return accessible() ? view.getAmount() : 0; }
        @Override public long getCapacity() { return accessible() ? view.getCapacity() : 0; }
        @Override public StorageView<FluidVariant> getUnderlyingView() { return view.getUnderlyingView(); }
        @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            check(resource, maxAmount);
            return accessible() ? view.extract(resource, Math.min(maxAmount, tank.rate()), transaction) : 0;
        }
    }
}
