package example.phaseteleporters;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/** Blocks a return trip until the player has completely left the destination plane. */
final class PortalReentryGuard {
    private static final double PLANE_MIN = 7.0 / 16.0;
    private static final double PLANE_MAX = 9.0 / 16.0;
    private static final Map<ServerPlayerEntity, ExitPortal> EXIT_PORTALS = new WeakHashMap<>();

    private PortalReentryGuard() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> EXIT_PORTALS.entrySet().removeIf(entry -> {
            ServerPlayerEntity player = entry.getKey();
            ExitPortal exit = entry.getValue();
            if (player == null || player.isRemoved() || player.getWorld() != exit.world()) return true;
            if (player.getBoundingBox().intersects(exit.volume)) {
                exit.seenAtExit = true;
                return false;
            }
            return exit.seenAtExit;
        }));
    }

    static boolean isBlocked(ServerPlayerEntity player) {
        return EXIT_PORTALS.containsKey(player);
    }

    static void mark(ServerPlayerEntity player, ServerWorld world, TeleportStructure.Bounds bounds) {
        BlockPos low = bounds.interiorMin();
        BlockPos high = bounds.interiorMax();
        Box volume = bounds.axis() == Direction.Axis.X
                ? new Box(low.getX(), low.getY(), low.getZ() + PLANE_MIN,
                        high.getX() + 1, high.getY() + 1, high.getZ() + PLANE_MAX)
                : new Box(low.getX() + PLANE_MIN, low.getY(), low.getZ(),
                        high.getX() + PLANE_MAX, high.getY() + 1, high.getZ() + 1);
        EXIT_PORTALS.put(player, new ExitPortal(world, volume));
    }

    static void clear(ServerPlayerEntity player) {
        EXIT_PORTALS.remove(player);
    }

    static boolean intersectsPlane(Box playerBox, BlockState state, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        Direction.Axis axis = state.get(Properties.HORIZONTAL_AXIS);
        Box planeBox = axis == Direction.Axis.X
                ? new Box(x, y, z + PLANE_MIN, x + 1, y + 1, z + PLANE_MAX)
                : new Box(x + PLANE_MIN, y, z, x + PLANE_MAX, y + 1, z + 1);
        return playerBox.intersects(planeBox);
    }

    private static final class ExitPortal {
        private final ServerWorld world;
        private final Box volume;
        private boolean seenAtExit;

        private ExitPortal(ServerWorld world, Box volume) {
            this.world = world;
            this.volume = volume;
        }

        private ServerWorld world() { return world; }
    }
}
