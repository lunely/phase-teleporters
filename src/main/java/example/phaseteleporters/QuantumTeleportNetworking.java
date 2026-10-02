package example.phaseteleporters;

import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class QuantumTeleportNetworking {
    private QuantumTeleportNetworking() {}
    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(QuantumTeleportFrequencyActionPayload.ID, QuantumTeleportFrequencyActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(QuantumTeleportFrequencySnapshotPayload.ID, QuantumTeleportFrequencySnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(QuantumTeleportFrequencyActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handle(context.player(), payload)));
    }
    private static void handle(ServerPlayerEntity player, QuantumTeleportFrequencyActionPayload payload) {
        if (!(player.currentScreenHandler instanceof QuantumTeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || handler.teleport() == null || !handler.canUse(player)
                || !(player.getWorld() instanceof ServerWorld world)) return;
        QuantumFrequencyState state = QuantumFrequencyState.get(world);
        String name = QuantumFrequencyState.normalize(payload.name());
        if (name.isEmpty()) return;
        UUID owner = player.getUuid();
        boolean privateType = payload.privateFrequency();
        switch (payload.action()) {
            case QuantumTeleportFrequencyActionPayload.CREATE -> {
                if (!state.contains(name, privateType, owner)
                        && !state.create(name, privateType, owner, player.getGameProfile().getName())) return;
                handler.teleport().setFrequency(name, privateType, owner);
            }
            case QuantumTeleportFrequencyActionPayload.SET -> {
                if (!state.contains(name, privateType, owner)) return;
                handler.teleport().setFrequency(name, privateType, owner);
            }
            case QuantumTeleportFrequencyActionPayload.DELETE -> {
                if (!state.delete(name, privateType, owner)) return;
            }
            case QuantumTeleportFrequencyActionPayload.COLOR -> {
                if (!handler.teleport().matchesFrequency(name, privateType, owner)
                        || !state.setColor(name, privateType, owner, payload.color())) return;
            }
            default -> { return; }
        }
        for (ServerPlayerEntity viewer : world.getServer().getPlayerManager().getPlayerList())
            if (viewer.currentScreenHandler instanceof QuantumTeleportScreenHandler screen
                    && screen.teleport() != null && screen.canUse(viewer)) sendSnapshot(viewer, screen.teleport());
    }
    public static void sendSnapshot(ServerPlayerEntity player, QuantumTeleportBlockEntity teleport) {
        if (!(player.currentScreenHandler instanceof QuantumTeleportScreenHandler handler)
                || handler.teleport() != teleport || !handler.canUse(player)
                || !(player.getWorld() instanceof ServerWorld world)) return;
        QuantumFrequencyState state = QuantumFrequencyState.get(world);
        if (!teleport.getFrequency().isEmpty() && !state.contains(teleport.getFrequency(),
                teleport.isPrivateFrequency(), teleport.getFrequencyOwner())) teleport.removeFrequency();
        UUID owner = player.getUuid();
        var publicEntries = state.visibleTo(owner, false);
        var privateEntries = state.visibleTo(owner, true);
        boolean hidden = teleport.isPrivateFrequency() && !owner.equals(teleport.getFrequencyOwner());
        ServerPlayNetworking.send(player, new QuantumTeleportFrequencySnapshotPayload(handler.syncId,
                hidden ? "" : teleport.getFrequency(), teleport.isPrivateFrequency(), hidden,
                hidden ? QuantumFrequencyState.DEFAULT_COLOR : state.color(teleport.getFrequency(),
                        teleport.isPrivateFrequency(), teleport.getFrequencyOwner()),
                publicEntries.stream().map(QuantumFrequencyState.Frequency::name).toList(),
                publicEntries.stream().map(QuantumFrequencyState.Frequency::color).toList(),
                publicEntries.stream().map(QuantumFrequencyState.Frequency::creatorName).toList(),
                privateEntries.stream().map(QuantumFrequencyState.Frequency::name).toList(),
                privateEntries.stream().map(QuantumFrequencyState.Frequency::color).toList(),
                privateEntries.stream().map(QuantumFrequencyState.Frequency::creatorName).toList()));
    }
}
