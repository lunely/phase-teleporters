package example.phaseteleports;

import java.util.UUID;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Checks a frequency only when a controller releases it. */
final class UnusedFrequencyCleanup {
    private UnusedFrequencyCleanup() {}

    static void local(ServerWorld world, String name, boolean privateFrequency, UUID owner) {
        LocalFrequencyState state = LocalFrequencyState.get(world);
        if (name.isEmpty() || !state.contains(name, privateFrequency, owner)) return;
        for (long packed : LocalTeleportIndex.get(world).positions()) {
            BlockPos pos = BlockPos.fromLong(packed);
            world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (!world.getBlockState(pos).isOf(PhaseTeleportsMod.TELEPORT)
                    || !(world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport)) {
                LocalTeleportIndex.get(world).remove(pos);
            } else if (teleport.matchesFrequency(name, privateFrequency, owner)) {
                return;
            }
        }
        state.delete(name, privateFrequency, owner);
        TeleportNetworking.syncOpenScreens(world);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.currentScreenHandler instanceof PortableTeleportScreenHandler)
                PortableTeleportNetworking.sendSnapshot(player);
        }
    }

    static void interdimensional(ServerWorld world, String name, boolean privateFrequency, UUID owner) {
        InterdimensionalFrequencyState state = InterdimensionalFrequencyState.get(world);
        if (name.isEmpty() || !state.contains(name, privateFrequency, owner)) return;
        for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
            for (long packed : InterdimensionalTeleportIndex.get(candidateWorld).positions()) {
                BlockPos pos = BlockPos.fromLong(packed);
                candidateWorld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (!candidateWorld.getBlockState(pos).isOf(PhaseTeleportsMod.INTERDIMENSIONAL_TELEPORT)
                        || !(candidateWorld.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport)) {
                    InterdimensionalTeleportIndex.get(candidateWorld).remove(pos);
                } else if (teleport.matchesFrequency(name, privateFrequency, owner)) {
                    return;
                }
            }
        }
        state.delete(name, privateFrequency, owner);
        InterdimensionalTeleportNetworking.syncOpenScreens(world);
        for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity player : candidateWorld.getPlayers()) {
                if (player.currentScreenHandler instanceof PortableTeleportScreenHandler)
                    PortableTeleportNetworking.sendSnapshot(player);
            }
        }
    }
}
