package example.phaseteleporters;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Blocks a return trip until the player has completely left the destination plane. */
final class PortalReentryGuard {
    private static final Map<ServerPlayerEntity, ExitPortal> EXIT_PORTALS = new WeakHashMap<>();

    private PortalReentryGuard() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> EXIT_PORTALS.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = entry.getKey();
            ExitPortal exit = entry.getValue();
            if (player == null || player.isRemoved() || player.getWorld() != exit.world()) return true;
            // Arrival can already be outside the plane, especially from a portable teleporter.
            return !player.getBoundingBox().intersects(exit.volume);
        }));
    }

    static boolean isBlocked(ServerPlayerEntity player) {
        return EXIT_PORTALS.containsKey(player);
    }

    static void mark(ServerPlayerEntity player, ServerWorld world, TeleportStructure.Bounds bounds) {
        BlockPos low = bounds.interiorMin();
        BlockPos high = bounds.interiorMax();
        // Keep the lock throughout the portal block, not just its thin rendered plane.
        Box volume = new Box(low.getX(), low.getY(), low.getZ(),
                high.getX() + 1, high.getY() + 1, high.getZ() + 1);
        EXIT_PORTALS.put(player, new ExitPortal(world, volume));
    }

    static void clear(ServerPlayerEntity player) {
        EXIT_PORTALS.remove(player);
    }

    static boolean intersectsPortalBlock(Box entityBox, BlockPos pos) {
        // Queue the transfer as soon as the entity enters the portal block;
        // waiting for its narrow visual plane adds a noticeable approach delay.
        return entityBox.intersects(new Box(pos));
    }

    private static final class ExitPortal {
        private final ServerWorld world;
        private final Box volume;
        private ExitPortal(ServerWorld world, Box volume) {
            this.world = world;
            this.volume = volume;
        }

        private ServerWorld world() { return world; }
    }
}
