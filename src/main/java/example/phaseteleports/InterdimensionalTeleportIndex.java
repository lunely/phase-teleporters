package example.phaseteleports;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

/** Positions of local Teleport blocks in one dimension. */
public final class InterdimensionalTeleportIndex extends PersistentState {
    private static final Type<InterdimensionalTeleportIndex> TYPE =
            new Type<>(InterdimensionalTeleportIndex::new, InterdimensionalTeleportIndex::fromNbt, null);
    private final Set<Long> positions = new HashSet<>();

    public static InterdimensionalTeleportIndex get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, "phaseteleports_interdimensional_teleports");
    }

    private static InterdimensionalTeleportIndex fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        InterdimensionalTeleportIndex index = new InterdimensionalTeleportIndex();
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
