package example.phaseteleports;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class InterdimensionalTerrainClient {
    private static volatile boolean active;

    private InterdimensionalTerrainClient() {}

    public static void register() {
        // The payload precedes vanilla's respawn packet on the same connection.
        ClientPlayNetworking.registerGlobalReceiver(InterdimensionalTerrainPayload.ID,
                (payload, context) -> active = payload.active());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> active = false);
    }

    public static boolean isActive() { return active; }
    public static void clear() { active = false; }
}
