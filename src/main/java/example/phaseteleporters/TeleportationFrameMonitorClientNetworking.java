package example.phaseteleporters;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class TeleportationFrameMonitorClientNetworking {
    private TeleportationFrameMonitorClientNetworking() {}
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(FrameMonitorOpenPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new FrameMonitorScreen(payload.pos(), payload.text()))));
    }
}
