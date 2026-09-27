package example.phaseteleports;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class PortableTeleportClientNetworking {
    private PortableTeleportClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PortableTeleportSnapshotPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().currentScreen instanceof PortableTeleportScreen screen) {
                        screen.applySnapshot(payload);
                    }
                }));
    }
}
