package example.phaseteleports;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TeleportClientNetworking {
    private TeleportClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TeleportFrequencySnapshotPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().currentScreen instanceof TeleportScreen screen) {
                        screen.applySnapshot(payload);
                    }
                }));
    }
}
