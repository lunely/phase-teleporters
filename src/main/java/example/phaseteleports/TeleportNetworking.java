package example.phaseteleports;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.UUID;

public final class TeleportNetworking {
    private TeleportNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(TeleportFrequencyActionPayload.ID,
                TeleportFrequencyActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TeleportFrequencySnapshotPayload.ID,
                TeleportFrequencySnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(TeleportFrequencyActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleAction(context.player(), payload)));
    }

    private static void handleAction(ServerPlayerEntity player, TeleportFrequencyActionPayload payload) {
        if (!(player.currentScreenHandler instanceof TeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || !handler.canUse(player)
                || handler.teleport() == null || !(player.getWorld() instanceof ServerWorld world)
                || handler.teleport().getWorld() != world
                || !handler.teleport().getWorld().getRegistryKey().equals(world.getRegistryKey())) return;
        LocalFrequencyState state = LocalFrequencyState.get(world);
        UUID owner = player.getUuid();
        boolean privateFrequency = payload.privateFrequency();
        String name = LocalFrequencyState.normalize(payload.name());
        if (name.isEmpty()) return;
        if (payload.action() == TeleportFrequencyActionPayload.DELETE) {
            if (!state.delete(name, privateFrequency, owner)) return;
            for (long packed : LocalTeleportIndex.get(world).positions()) {
                BlockPos pos = BlockPos.fromLong(packed);
                world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport
                        && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.removeFrequency();
            }
            if (handler.teleport().matchesFrequency(name, privateFrequency, owner))
                handler.teleport().removeFrequency();
            syncOpenScreens(world);
        } else if (payload.action() == TeleportFrequencyActionPayload.CREATE) {
            if (!state.contains(name, privateFrequency, owner)
                    && !state.create(name, privateFrequency, owner, player.getGameProfile().getName())) return;
            handler.teleport().setFrequency(name, privateFrequency, owner);
            syncOpenScreens(world);
        } else if (payload.action() == TeleportFrequencyActionPayload.SET
                && state.contains(name, privateFrequency, owner)) {
            handler.teleport().setFrequency(name, privateFrequency, owner);
            syncOpenScreens(world);
        } else if (payload.action() == TeleportFrequencyActionPayload.COLOR
                && handler.teleport().matchesFrequency(name, privateFrequency, owner)
                && state.setColor(name, privateFrequency, owner, payload.color())) {
            for (long packed : LocalTeleportIndex.get(world).positions()) {
                BlockPos pos = BlockPos.fromLong(packed);
                world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport
                        && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.refreshPortal();
            }
            syncOpenScreens(world);
        }
    }

    public static void sendSnapshot(ServerPlayerEntity player, TeleportBlockEntity teleport) {
        if (!(player.currentScreenHandler instanceof TeleportScreenHandler handler)
                || handler.teleport() != teleport || !(player.getWorld() instanceof ServerWorld world)
                || teleport.getWorld() != world
                || !teleport.getWorld().getRegistryKey().equals(world.getRegistryKey())) return;
        LocalFrequencyState state = LocalFrequencyState.get(world);
        UUID owner = player.getUuid();
        List<LocalFrequencyState.Frequency> publicFrequencies = state.visibleTo(owner, false);
        List<LocalFrequencyState.Frequency> privateFrequencies = state.visibleTo(owner, true);
        boolean assignedPrivate = teleport.isPrivateFrequency();
        boolean hidden = assignedPrivate && !owner.equals(teleport.getFrequencyOwner());
        String assigned = hidden ? "" : teleport.getFrequency();
        int color = hidden ? PortalColors.DEFAULT : state.color(teleport.getFrequency(),
                assignedPrivate, teleport.getFrequencyOwner());
        ServerPlayNetworking.send(player, new TeleportFrequencySnapshotPayload(
                handler.syncId, assigned, assignedPrivate, hidden, color,
                publicFrequencies.stream().map(LocalFrequencyState.Frequency::name).toList(),
                publicFrequencies.stream().map(LocalFrequencyState.Frequency::color).toList(),
                publicFrequencies.stream().map(LocalFrequencyState.Frequency::creatorName).toList(),
                privateFrequencies.stream().map(LocalFrequencyState.Frequency::name).toList(),
                privateFrequencies.stream().map(LocalFrequencyState.Frequency::color).toList(),
                privateFrequencies.stream().map(LocalFrequencyState.Frequency::creatorName).toList()));
    }

    static void syncOpenScreens(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.currentScreenHandler instanceof TeleportScreenHandler handler
                    && handler.teleport() != null && handler.canUse(player)) {
                sendSnapshot(player, handler.teleport());
            }
        }
    }
}
