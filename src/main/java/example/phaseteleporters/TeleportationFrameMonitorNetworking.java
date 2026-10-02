package example.phaseteleporters;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TeleportationFrameMonitorNetworking {
    private TeleportationFrameMonitorNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(FrameMonitorTextPayload.ID, FrameMonitorTextPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(FrameMonitorOpenPayload.ID, FrameMonitorOpenPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(FrameMonitorTextPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (!player.getAbilities().allowModifyWorld
                            || player.squaredDistanceTo(payload.pos().toCenterPos()) > 64
                            || !(player.getWorld().getBlockEntity(payload.pos())
                                    instanceof TeleportationFrameMonitorBlockEntity monitor)) return;
                    monitor.finishEditing(player.getUuid(), payload.text());
                }));
    }

    public static void open(ServerPlayerEntity player, TeleportationFrameMonitorBlockEntity monitor) {
        if (player.squaredDistanceTo(monitor.getPos().toCenterPos()) > 64) return;
        if (!player.getAbilities().allowModifyWorld || !monitor.beginEditing(player.getUuid())) return;
        ServerPlayNetworking.send(player, new FrameMonitorOpenPayload(monitor.getPos(), monitor.text()));
    }
}
