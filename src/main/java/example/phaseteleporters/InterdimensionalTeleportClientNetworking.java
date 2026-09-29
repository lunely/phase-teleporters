package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class InterdimensionalTeleportClientNetworking {
    private InterdimensionalTeleportClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(InterdimensionalTeleportFrequencySnapshotPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().currentScreen instanceof InterdimensionalTeleportScreen screen) {
                        screen.applySnapshot(payload);
                    }
                }));
    }
}
