package example.phaseteleports;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class PortableTeleportNetworking {
    private static final long REQUIRED_TARGET_ENERGY = 500;
    private static final long TELEPORT_COST = 2_000;
    private PortableTeleportNetworking() {}

    private record Destination(ServerWorld world, BlockPos controller, Vec3d arrival) {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(PortableTeleportActionPayload.ID,
                PortableTeleportActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PortableTeleportSnapshotPayload.ID,
                PortableTeleportSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PortableTeleportActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleAction(context.player(), payload)));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof PortableTeleportScreenHandler handler)
                || !handler.canUse(player) || !(player.getWorld() instanceof ServerWorld world)) return;
        UUID owner = player.getUuid();
        LocalFrequencyState local = LocalFrequencyState.get(world);
        InterdimensionalFrequencyState interdimensional = InterdimensionalFrequencyState.get(world);
        ServerPlayNetworking.send(player, new PortableTeleportSnapshotPayload(handler.syncId,
                localEntries(local.visibleTo(owner, false)), localEntries(local.visibleTo(owner, true)),
                interdimensionalEntries(interdimensional.visibleTo(owner, false)),
                interdimensionalEntries(interdimensional.visibleTo(owner, true))));
    }

    private static List<PortableTeleportSnapshotPayload.Entry> localEntries(
            List<LocalFrequencyState.Frequency> frequencies) {
        return frequencies.stream().map(f -> new PortableTeleportSnapshotPayload.Entry(
                f.name(), f.color(), f.creatorName())).toList();
    }

    private static List<PortableTeleportSnapshotPayload.Entry> interdimensionalEntries(
            List<InterdimensionalFrequencyState.Frequency> frequencies) {
        return frequencies.stream().map(f -> new PortableTeleportSnapshotPayload.Entry(
                f.name(), f.color(), f.creatorName())).toList();
    }

    private static void handleAction(ServerPlayerEntity player, PortableTeleportActionPayload payload) {
        if (!(player.currentScreenHandler instanceof PortableTeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || !handler.canUse(player)
                || !(player.getWorld() instanceof ServerWorld sourceWorld)) return;
        ItemStack stack = handler.getUsedStack(player);
        if (!(stack.getItem() instanceof PortableTeleportItem portable)
                || portable.getStoredPE(stack) < TELEPORT_COST) return;
        String name = LocalFrequencyState.normalize(payload.name());
        if (name.isEmpty()) return;
        UUID owner = player.getUuid();
        if (payload.interdimensional()) {
            if (!InterdimensionalFrequencyState.get(sourceWorld)
                    .contains(name, payload.privateFrequency(), owner)) return;
            Destination destination = findInterdimensional(player, sourceWorld, player.getBlockPos(),
                    name, payload.privateFrequency(), owner);
            if (destination == null) return;
            teleportInterdimensional(player, sourceWorld, destination, stack, portable);
        } else {
            if (!LocalFrequencyState.get(sourceWorld)
                    .contains(name, payload.privateFrequency(), owner)) return;
            Destination destination = findLocal(player, sourceWorld, player.getBlockPos(),
                    name, payload.privateFrequency(), owner);
            if (destination == null) return;
            teleportLocal(player, sourceWorld, destination, stack, portable);
        }
    }

    private static Destination findLocal(ServerPlayerEntity player, ServerWorld world, BlockPos from,
            String name, boolean privateFrequency, UUID owner) {
        Destination best = null;
        long bestDistance = Long.MAX_VALUE;
        for (long packed : LocalTeleportIndex.get(world).positions()) {
            BlockPos pos = BlockPos.fromLong(packed);
            world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (!(world.getBlockEntity(pos) instanceof TeleportBlockEntity candidate)) {
                LocalTeleportIndex.get(world).remove(pos);
                continue;
            }
            if (!candidate.matchesFrequency(name, privateFrequency, owner)
                    || candidate.getStored() < REQUIRED_TARGET_ENERGY) continue;
            Vec3d arrival = findSafeArrival(player, world, pos);
            if (arrival == null) continue;
            long distance = distanceSquared(from, pos);
            if (distance < bestDistance) {
                best = new Destination(world, pos, arrival);
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Destination findInterdimensional(ServerPlayerEntity player, ServerWorld sourceWorld,
            BlockPos from, String name, boolean privateFrequency, UUID owner) {
        Destination best = null;
        long bestDistance = Long.MAX_VALUE;
        for (ServerWorld world : sourceWorld.getServer().getWorlds()) {
            for (long packed : InterdimensionalTeleportIndex.get(world).positions()) {
                BlockPos pos = BlockPos.fromLong(packed);
                world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                if (!(world.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity candidate)) {
                    InterdimensionalTeleportIndex.get(world).remove(pos);
                    continue;
                }
                if (!candidate.matchesFrequency(name, privateFrequency, owner)
                        || candidate.getStored() < REQUIRED_TARGET_ENERGY) continue;
                Vec3d arrival = findSafeArrival(player, world, pos);
                if (arrival == null) continue;
                long distance = distanceSquared(from, pos);
                boolean otherDimension = world != sourceWorld;
                boolean bestOtherDimension = best != null && best.world() != sourceWorld;
                if (best == null || (otherDimension && !bestOtherDimension)
                        || (otherDimension == bestOtherDimension && distance < bestDistance)) {
                    best = new Destination(world, pos, arrival);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long) a.getX() - b.getX();
        long dy = (long) a.getY() - b.getY();
        long dz = (long) a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vec3d findSafeArrival(ServerPlayerEntity player, ServerWorld world, BlockPos controller) {
        BlockPos feet = controller.up();
        if (!world.getFluidState(feet).isEmpty() || !world.getFluidState(feet.up()).isEmpty()) return null;
        double x = controller.getX() + 0.5;
        double z = controller.getZ() + 0.5;
        double halfWidth = player.getWidth() / 2.0;
        Box space = new Box(x - halfWidth, feet.getY(), z - halfWidth,
                x + halfWidth, feet.getY() + player.getHeight(), z + halfWidth);
        return world.isSpaceEmpty(player, space) ? new Vec3d(x, feet.getY(), z) : null;
    }

    private static void teleportLocal(ServerPlayerEntity player, ServerWorld world, Destination destination,
            ItemStack stack, PortableTeleportItem portable) {
        if (!(world.getBlockEntity(destination.controller()) instanceof TeleportBlockEntity controller)
                || controller.getStored() < REQUIRED_TARGET_ENERGY
                || portable.getStoredPE(stack) < TELEPORT_COST) return;
        Vec3d arrival = destination.arrival();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();
        TeleportStructure.Bounds bounds = controller.getStructureBounds().orElse(null);
        if (bounds != null) PortalReentryGuard.mark(player, world, bounds);
        if (player.teleport(world, arrival.x, arrival.y, arrival.z,
                java.util.Set.of(), player.getYaw(), player.getPitch())) {
            portable.spendPE(stack, TELEPORT_COST);
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            player.closeHandledScreen();
            PortalPlaneBlock.playTeleportEffects(world, world,
                    sourceX, sourceY, sourceZ, arrival.x, arrival.y, arrival.z);
        } else if (bounds != null) {
            PortalReentryGuard.clear(player);
        }
    }

    private static void teleportInterdimensional(ServerPlayerEntity player, ServerWorld sourceWorld,
            Destination destination, ItemStack stack, PortableTeleportItem portable) {
        ServerWorld targetWorld = destination.world();
        if (!(targetWorld.getBlockEntity(destination.controller())
                instanceof InterdimensionalTeleportBlockEntity controller)
                || controller.getStored() < REQUIRED_TARGET_ENERGY
                || portable.getStoredPE(stack) < TELEPORT_COST) return;
        Vec3d arrival = destination.arrival();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();
        InterdimensionalTeleportStructure.Bounds bounds = controller.getStructureBounds().orElse(null);
        if (bounds != null) InterdimensionalReentryGuard.mark(player, sourceWorld, targetWorld, bounds);
        if (sourceWorld != targetWorld)
            ServerPlayNetworking.send(player, new InterdimensionalTerrainPayload(true));
        boolean teleported = player.teleport(targetWorld, arrival.x, arrival.y, arrival.z,
                java.util.Set.of(), player.getYaw(), player.getPitch());
        if (teleported && player.getWorld() == targetWorld) {
            portable.spendPE(stack, TELEPORT_COST);
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            if (bounds != null) InterdimensionalReentryGuard.transferCompleted(player, sourceWorld, targetWorld);
            player.closeHandledScreen();
            PortalPlaneBlock.playTeleportEffects(sourceWorld, targetWorld,
                    sourceX, sourceY, sourceZ, arrival.x, arrival.y, arrival.z);
        } else {
            if (sourceWorld != targetWorld)
                ServerPlayNetworking.send(player, new InterdimensionalTerrainPayload(false));
            if (bounds != null) InterdimensionalReentryGuard.clear(player, "portable-teleport-failed");
        }
    }
}
