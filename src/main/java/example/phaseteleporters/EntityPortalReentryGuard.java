package example.phaseteleporters;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** Prevents non-player entities from immediately returning through their arrival portal. */
final class EntityPortalReentryGuard {
    private static final Map<UUID, ExitPortal> EXITS = new HashMap<>();

    private EntityPortalReentryGuard() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> EXITS.entrySet().removeIf(entry -> {
            ExitPortal exit = entry.getValue();
            Entity entity = exit.world.getEntity(entry.getKey());
            if (entity == null || entity.isRemoved()) return ++exit.missingTicks > 40;
            exit.missingTicks = 0;
            return !entity.getBoundingBox().intersects(exit.volume);
        }));
    }

    static boolean isBlocked(Entity entity) {
        return EXITS.containsKey(entity.getUuid());
    }

    static void mark(Entity entity, ServerWorld destination, BlockPos low, BlockPos high) {
        EXITS.put(entity.getUuid(), new ExitPortal(destination,
                new Box(low.getX(), low.getY(), low.getZ(),
                        high.getX() + 1, high.getY() + 1, high.getZ() + 1)));
    }

    static void clear(Entity entity) {
        EXITS.remove(entity.getUuid());
    }

    private static final class ExitPortal {
        private final ServerWorld world;
        private final Box volume;
        private int missingTicks;

        private ExitPortal(ServerWorld world, Box volume) {
            this.world = world;
            this.volume = volume;
        }
    }
}
