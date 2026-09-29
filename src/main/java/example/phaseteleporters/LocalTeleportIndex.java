package example.phaseteleporters;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/** Positions of local Teleport blocks in one dimension. */
public final class LocalTeleportIndex extends PersistentState {
    private static final Type<LocalTeleportIndex> TYPE =
            new Type<>(LocalTeleportIndex::new, LocalTeleportIndex::fromNbt, null);
    private final Set<Long> positions = new HashSet<>();

    public static LocalTeleportIndex get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, "phaseteleporters_local_teleports");
    }

    private static LocalTeleportIndex fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        LocalTeleportIndex index = new LocalTeleportIndex();
        for (long packed : nbt.getLongArray("Positions")) index.positions.add(packed);
        return index;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putLongArray("Positions", positions.stream().mapToLong(Long::longValue).toArray());
        return nbt;
    }

    public Set<Long> positions() { return Set.copyOf(positions); }
    public void add(BlockPos pos) { if (positions.add(pos.asLong())) markDirty(); }
    public void remove(BlockPos pos) { if (positions.remove(pos.asLong())) markDirty(); }
}
