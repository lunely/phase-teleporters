package example.phaseteleports;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Keeps a cross-dimension return lock until the player leaves the whole exit portal. */
final class InterdimensionalReentryGuard {
    private static final Logger LOGGER = LoggerFactory.getLogger("phaseteleports/interdimensional_teleport");
    // Temporary diagnostics can be enabled with -Dphaseteleports.debugInterdimensionalTeleport=true.
    private static final boolean DEBUG = Boolean.getBoolean("phaseteleports.debugInterdimensionalTeleport");
    private static final Map<UUID, ExitPortal> EXITS = new HashMap<>();
    private static final Map<UUID, String> LAST_COLLISION_STATE = new HashMap<>();

    private InterdimensionalReentryGuard() {}

    static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                clear(handler.player, "disconnected"));
        ServerTickEvents.END_SERVER_TICK.register(server -> EXITS.entrySet().removeIf(entry -> {
            UUID id = entry.getKey();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player == null) return false;
            // A player can be temporarily removed while vanilla changes dimensions.
            if (player.isRemoved()) return false;
            ExitPortal exit = entry.getValue();
            RegistryKey<World> currentDimension = player.getWorld().getRegistryKey();
            if (!currentDimension.equals(exit.dimension())) {
                if (exit.seenAtExit()) {
                    log("clear uuid={} reason=left-dimension source={} destination={}",
                            id, currentDimension.getValue(), exit.dimension().getValue());
                    return true;
                }
                return false;
            }
            boolean inside = player.getBoundingBox().intersects(exit.volume());
            if (inside) {
                if (!exit.seenAtExit()) {
                    entry.setValue(exit.withSeenAtExit());
                    log("arrival uuid={} dimension={} box={} exit={}", id,
                            currentDimension.getValue(), player.getBoundingBox(), exit.volume());
                }
                return false;
            }
            if (!exit.seenAtExit()) return false;
            log("clear uuid={} reason=fully-left-exit dimension={} box={} exit={}", id,
                    currentDimension.getValue(), player.getBoundingBox(), exit.volume());
            return true;
        }));
    }

    static boolean isBlocked(ServerPlayerEntity player) {
        return EXITS.containsKey(player.getUuid());
    }

    static void collision(ServerPlayerEntity player, ServerWorld source, BlockPos planePos,
            boolean blocked) {
        if (!DEBUG) return;
        String state = source.getRegistryKey().getValue() + "/" + blocked;
        if (state.equals(LAST_COLLISION_STATE.put(player.getUuid(), state))) return;
        ExitPortal exit = EXITS.get(player.getUuid());
        log("collision uuid={} source={} plane={} isBlocked={} destination={} seenAtExit={} box={}",
                player.getUuid(), source.getRegistryKey().getValue(), planePos, blocked,
                exit == null ? "-" : exit.dimension().getValue(),
                exit != null && exit.seenAtExit(), player.getBoundingBox());
    }

    static void mark(ServerPlayerEntity player, ServerWorld source, ServerWorld destination,
            InterdimensionalTeleportStructure.Bounds bounds) {
        BlockPos low = bounds.interiorMin();
        BlockPos high = bounds.interiorMax();
        Box volume = new Box(low.getX(), low.getY(), low.getZ(),
                high.getX() + 1, high.getY() + 1, high.getZ() + 1);
        EXITS.put(player.getUuid(), new ExitPortal(destination.getRegistryKey(), volume, false));
        log("mark uuid={} source={} destination={} exit={}", player.getUuid(),
                source.getRegistryKey().getValue(), destination.getRegistryKey().getValue(), volume);
    }

    static void transferCompleted(ServerPlayerEntity player, ServerWorld source, ServerWorld destination) {
        log("transfer-completed uuid={} source={} destination={} current={} box={}", player.getUuid(),
                source.getRegistryKey().getValue(), destination.getRegistryKey().getValue(),
                player.getWorld().getRegistryKey().getValue(), player.getBoundingBox());
    }

    static void clear(ServerPlayerEntity player, String reason) {
        ExitPortal exit = EXITS.remove(player.getUuid());
        LAST_COLLISION_STATE.remove(player.getUuid());
        if (exit != null) log("clear uuid={} reason={} current={} destination={}", player.getUuid(),
                reason, player.getWorld().getRegistryKey().getValue(), exit.dimension().getValue());
    }

    private static void log(String message, Object... values) {
        if (DEBUG) LOGGER.info(message, values);
    }

    private record ExitPortal(RegistryKey<World> dimension, Box volume, boolean seenAtExit) {
        ExitPortal withSeenAtExit() { return new ExitPortal(dimension, volume, true); }
    }
}
