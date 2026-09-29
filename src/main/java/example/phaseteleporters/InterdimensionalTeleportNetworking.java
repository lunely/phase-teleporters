package example.phaseteleporters;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.List;
import java.util.UUID;

public final class InterdimensionalTeleportNetworking {
    private InterdimensionalTeleportNetworking() {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(InterdimensionalTeleportFrequencyActionPayload.ID,
                InterdimensionalTeleportFrequencyActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InterdimensionalTeleportFrequencySnapshotPayload.ID,
                InterdimensionalTeleportFrequencySnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(InterdimensionalTerrainPayload.ID,
                InterdimensionalTerrainPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(InterdimensionalTeleportFrequencyActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleAction(context.player(), payload)));
    }

    private static void handleAction(ServerPlayerEntity player, InterdimensionalTeleportFrequencyActionPayload payload) {
        if (!(player.currentScreenHandler instanceof InterdimensionalTeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || !handler.canUse(player)
                || handler.teleport() == null || !(player.getWorld() instanceof ServerWorld world)
                || handler.teleport().getWorld() != world
                || !handler.teleport().getWorld().getRegistryKey().equals(world.getRegistryKey())) return;
        InterdimensionalFrequencyState state = InterdimensionalFrequencyState.get(world);
        UUID owner = player.getUuid();
        boolean privateFrequency = payload.privateFrequency();
        String name = InterdimensionalFrequencyState.normalize(payload.name());
        if (name.isEmpty()) return;
        if (payload.action() == InterdimensionalTeleportFrequencyActionPayload.DELETE) {
            if (!state.delete(name, privateFrequency, owner)) return;
            for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
                for (long packed : InterdimensionalTeleportIndex.get(candidateWorld).positions()) {
                    BlockPos pos = BlockPos.fromLong(packed);
                    candidateWorld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    if (candidateWorld.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport
                            && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.removeFrequency();
                }
            }
            if (handler.teleport().matchesFrequency(name, privateFrequency, owner))
                handler.teleport().removeFrequency();
            syncOpenScreens(world);
        } else if (payload.action() == InterdimensionalTeleportFrequencyActionPayload.CREATE) {
            if (!state.contains(name, privateFrequency, owner)
                    && !state.create(name, privateFrequency, owner, player.getGameProfile().getName())) return;
            handler.teleport().setFrequency(name, privateFrequency, owner);
            syncOpenScreens(world);
        } else if (payload.action() == InterdimensionalTeleportFrequencyActionPayload.SET
                && state.contains(name, privateFrequency, owner)) {
            handler.teleport().setFrequency(name, privateFrequency, owner);
            syncOpenScreens(world);
        } else if (payload.action() == InterdimensionalTeleportFrequencyActionPayload.COLOR
                && handler.teleport().matchesFrequency(name, privateFrequency, owner)
                && state.setColor(name, privateFrequency, owner, payload.color())) {
            for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
                for (long packed : InterdimensionalTeleportIndex.get(candidateWorld).positions()) {
                    BlockPos pos = BlockPos.fromLong(packed);
                    candidateWorld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    if (candidateWorld.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport
                            && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.refreshPortal();
                }
            }
            syncOpenScreens(world);
        }
    }

    public static void sendSnapshot(ServerPlayerEntity player, InterdimensionalTeleportBlockEntity teleport) {
        if (!(player.currentScreenHandler instanceof InterdimensionalTeleportScreenHandler handler)
                || handler.teleport() != teleport || !(player.getWorld() instanceof ServerWorld world)
                || teleport.getWorld() != world
                || !teleport.getWorld().getRegistryKey().equals(world.getRegistryKey())) return;
        InterdimensionalFrequencyState state = InterdimensionalFrequencyState.get(world);
        UUID owner = player.getUuid();
        List<InterdimensionalFrequencyState.Frequency> publicFrequencies = state.visibleTo(owner, false);
        List<InterdimensionalFrequencyState.Frequency> privateFrequencies = state.visibleTo(owner, true);
        boolean assignedPrivate = teleport.isPrivateFrequency();
        boolean hidden = assignedPrivate && !owner.equals(teleport.getFrequencyOwner());
        String assigned = hidden ? "" : teleport.getFrequency();
        int color = hidden ? InterdimensionalFrequencyState.DEFAULT_COLOR : state.color(teleport.getFrequency(),
                assignedPrivate, teleport.getFrequencyOwner());
        ServerPlayNetworking.send(player, new InterdimensionalTeleportFrequencySnapshotPayload(
                handler.syncId, assigned, assignedPrivate, hidden, color,
                publicFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::name).toList(),
                publicFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::color).toList(),
                publicFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::creatorName).toList(),
                privateFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::name).toList(),
                privateFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::color).toList(),
                privateFrequencies.stream().map(InterdimensionalFrequencyState.Frequency::creatorName).toList()));
    }

    static void syncOpenScreens(ServerWorld world) {
        for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
            for (ServerPlayerEntity player : candidateWorld.getPlayers()) {
                if (player.currentScreenHandler instanceof InterdimensionalTeleportScreenHandler handler
                        && handler.teleport() != null && handler.canUse(player)) {
                    sendSnapshot(player, handler.teleport());
                }
            }
        }
    }
}
