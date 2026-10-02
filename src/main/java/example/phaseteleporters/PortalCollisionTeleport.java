package example.phaseteleporters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;

/** Runs portal transfers after vanilla finishes processing movement and block collisions. */
final class PortalCollisionTeleport {
    private static final Map<UUID, Runnable> PENDING = new LinkedHashMap<>();

    private PortalCollisionTeleport() {}

    static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                for (Runnable transfer : List.copyOf(PENDING.values())) transfer.run();
            } finally {
                PENDING.clear();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    static void enqueue(Entity entity, Runnable transfer) {
        // A tall entity touches several plane blocks; riders share one transfer with their vehicle.
        PENDING.putIfAbsent(entity.getRootVehicle().getUuid(), transfer);
    }
}
