package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class QuantumTeleportClientNetworking {
    private QuantumTeleportClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(QuantumTeleportFrequencySnapshotPayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().currentScreen instanceof QuantumTeleportScreen screen) {
                        screen.applySnapshot(payload);
                    }
                }));
    }
}
