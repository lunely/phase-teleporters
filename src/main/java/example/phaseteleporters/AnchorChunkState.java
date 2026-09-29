package example.phaseteleporters;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;

/** Tracks portal-owned forced chunks without releasing another portal's anchor. */
public final class AnchorChunkState extends PersistentState {
    private static final Type<AnchorChunkState> TYPE =
            new Type<>(AnchorChunkState::new, AnchorChunkState::fromNbt, null);
    private final Set<Long> controllers = new HashSet<>();
    private final Set<Long> ownedChunks = new HashSet<>();

    private static AnchorChunkState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, "phaseteleporters_anchor_chunks");
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20 == 0) get(world).validate(world);
        });
    }

    public static void setActive(ServerWorld world, BlockPos controller, boolean active) {
        AnchorChunkState state = get(world);
        if (active) state.add(world, controller);
        else state.remove(world, controller);
    }

    private void add(ServerWorld world, BlockPos controller) {
        long packed = controller.asLong();
        if (!controllers.add(packed)) return;
        markDirty();
        ChunkPos chunk = new ChunkPos(controller);
        long chunkKey = chunk.toLong();
        if (!world.getForcedChunks().contains(chunkKey)) {
            world.setChunkForced(chunk.x, chunk.z, true);
            ownedChunks.add(chunkKey);
            markDirty();
        }
    }

    private void remove(ServerWorld world, BlockPos controller) {
        if (!controllers.remove(controller.asLong())) return;
        markDirty();
        ChunkPos chunk = new ChunkPos(controller);
        long chunkKey = chunk.toLong();
        boolean another = controllers.stream()
                .anyMatch(packed -> new ChunkPos(BlockPos.fromLong(packed)).toLong() == chunkKey);
        if (!another && ownedChunks.remove(chunkKey)) {
            world.setChunkForced(chunk.x, chunk.z, false);
            markDirty();
        }
    }

    private void validate(ServerWorld world) {
        for (long packed : Set.copyOf(controllers)) {
            BlockPos pos = BlockPos.fromLong(packed);
            ChunkPos chunk = new ChunkPos(pos);
            long chunkKey = chunk.toLong();
            if (!world.getForcedChunks().contains(chunkKey)) {
                world.setChunkForced(chunk.x, chunk.z, true);
                if (ownedChunks.add(chunkKey)) markDirty();
            }
            if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) continue;
            if (!(world.getBlockEntity(pos) instanceof AnchoredTeleportBlockEntity teleport)
                    || !teleport.hasAnchorUpgrade()) remove(world, pos);
        }
    }

    private static AnchorChunkState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        AnchorChunkState state = new AnchorChunkState();
        for (long value : nbt.getLongArray("Controllers")) state.controllers.add(value);
        for (long value : nbt.getLongArray("OwnedChunks")) state.ownedChunks.add(value);
        return state;
    }

    @Override public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLongArray("Controllers", controllers.stream().mapToLong(Long::longValue).toArray());
        nbt.putLongArray("OwnedChunks", ownedChunks.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }
}
