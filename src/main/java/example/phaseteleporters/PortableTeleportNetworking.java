package example.phaseteleporters;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class PortableTeleportNetworking {
    private static final long REQUIRED_TARGET_ENERGY = 500;
    private static final long TELEPORT_COST = 2_000;
    private PortableTeleportNetworking() {}

    private record Destination(ServerWorld world, BlockPos controller, Vec3d arrival) {}

    public static void registerServer() {
        PayloadTypeRegistry.playC2S().register(PortableModePayload.ID, PortableModePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PortableModePayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player.currentScreenHandler != player.playerScreenHandler
                            || player.getInventory().selectedSlot != payload.slot()
                            || payload.mode() < 0 || payload.mode() >= PortableTeleportItem.Mode.values().length) return;
                    // Sneak is checked against vanilla client input before sending;
                    // its movement packet may reach the server after the wheel packet.
                    ItemStack stack = player.getMainHandStack();
                    if (!(stack.getItem() instanceof PortableTeleportItem portable)) return;
                    var mode = PortableTeleportItem.Mode.byId(payload.mode());
                    portable.setMode(stack, mode);
                    player.getInventory().markDirty();
                    player.playerScreenHandler.sendContentUpdates();
                    player.sendMessage(Text.translatable(mode.messageKey()), true);
                }));
        PayloadTypeRegistry.playC2S().register(PortableTeleportActionPayload.ID,
                PortableTeleportActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortableFrequencyActionPayload.ID,
                PortableFrequencyActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortableSelectionPayload.ID,
                PortableSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortableSnapshotRequestPayload.ID,
                PortableSnapshotRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PortableTeleportSnapshotPayload.ID,
                PortableTeleportSnapshotPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PortableTeleportActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleAction(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(PortableFrequencyActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleFrequencyAction(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(PortableSelectionPayload.ID,
                (payload, context) -> context.server().execute(() -> handleSelection(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(PortableSnapshotRequestPayload.ID,
                (payload, context) -> context.server().execute(() -> {
                    if (context.player().currentScreenHandler instanceof PortableTeleportScreenHandler handler
                            && handler.syncId == payload.syncId()) sendSnapshot(context.player());
                }));
    }

    public static void sendSnapshot(ServerPlayerEntity player) {
        if (!(player.currentScreenHandler instanceof PortableTeleportScreenHandler handler)
                || !handler.canUse(player) || !(player.getWorld() instanceof ServerWorld world)) return;
        ItemStack stack = handler.getUsedStack(player);
        if (!(stack.getItem() instanceof PortableTeleportItem portable)) return;
        UUID owner = player.getUuid();
        LocalFrequencyState local = LocalFrequencyState.get(world);
        InterdimensionalFrequencyState interdimensional = InterdimensionalFrequencyState.get(world);
        PortableTeleportItem.Selection saved = portable.getSelection(stack);
        PortableTeleportItem.View view = PortableTeleportViewState.get(world)
                .viewFor(owner, portable.getView(stack));
        ServerPlayNetworking.send(player, new PortableTeleportSnapshotPayload(handler.syncId,
                localEntries(local.visibleTo(owner, false)), localEntries(local.visibleTo(owner, true)),
                interdimensionalEntries(interdimensional.visibleTo(owner, false)),
                interdimensionalEntries(interdimensional.visibleTo(owner, true)),
                saved.name(), saved.privateFrequency(), saved.interdimensional(),
                view.privateFrequency(), view.interdimensional()));
    }

    private static List<PortableTeleportSnapshotPayload.Entry> localEntries(
            List<LocalFrequencyState.Frequency> frequencies) {
        return frequencies.stream().map(f -> new PortableTeleportSnapshotPayload.Entry(
                f.name(), f.creatorName())).toList();
    }

    private static List<PortableTeleportSnapshotPayload.Entry> interdimensionalEntries(
            List<InterdimensionalFrequencyState.Frequency> frequencies) {
        return frequencies.stream().map(f -> new PortableTeleportSnapshotPayload.Entry(
                f.name(), f.creatorName())).toList();
    }

    private static void saveSelection(ServerPlayerEntity player, PortableTeleportScreenHandler handler,
            ItemStack stack, PortableTeleportItem portable, PortableTeleportItem.Selection selection) {
        portable.setSelection(stack, selection);
        player.getInventory().markDirty();
        if (handler != null) handler.sendContentUpdates();
        player.playerScreenHandler.sendContentUpdates();
    }

    private static void handleSelection(ServerPlayerEntity player, PortableSelectionPayload payload) {
        if (!(player.currentScreenHandler instanceof PortableTeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || !handler.canUse(player)
                || !(player.getWorld() instanceof ServerWorld world)) return;
        ItemStack stack = handler.getUsedStack(player);
        if (!(stack.getItem() instanceof PortableTeleportItem)) return;
        PortableTeleportViewState.get(world).setView(player.getUuid(),
                new PortableTeleportItem.View(payload.privateFrequency(), payload.interdimensional()));
    }

    private static void handleFrequencyAction(ServerPlayerEntity player, PortableFrequencyActionPayload payload) {
        if (!(player.currentScreenHandler instanceof PortableTeleportScreenHandler handler)
                || handler.syncId != payload.syncId() || !handler.canUse(player)
                || !(player.getWorld() instanceof ServerWorld world)) return;
        ItemStack stack = handler.getUsedStack(player);
        if (!(stack.getItem() instanceof PortableTeleportItem portable)) return;
        String name = LocalFrequencyState.normalize(payload.name());
        if (name.isEmpty()) return;
        UUID owner = player.getUuid();
        boolean privateFrequency = payload.privateFrequency();
        if (payload.interdimensional()) {
            InterdimensionalFrequencyState state = InterdimensionalFrequencyState.get(world);
            if (payload.action() == PortableFrequencyActionPayload.CREATE) {
                if (!state.create(name, privateFrequency, owner, player.getGameProfile().getName())) return;
            } else if (payload.action() == PortableFrequencyActionPayload.DELETE) {
                if (!state.delete(name, privateFrequency, owner)) return;
                clearDeletedSelection(player, handler, stack, portable, name, privateFrequency, true);
                for (ServerWorld candidateWorld : world.getServer().getWorlds()) {
                    for (long packed : InterdimensionalTeleportIndex.get(candidateWorld).positions()) {
                        BlockPos pos = BlockPos.fromLong(packed);
                        candidateWorld.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                        if (candidateWorld.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport
                                && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.removeFrequency();
                    }
                }
            } else return;
            InterdimensionalTeleportNetworking.syncOpenScreens(world);
        } else {
            LocalFrequencyState state = LocalFrequencyState.get(world);
            if (payload.action() == PortableFrequencyActionPayload.CREATE) {
                if (!state.create(name, privateFrequency, owner, player.getGameProfile().getName())) return;
            } else if (payload.action() == PortableFrequencyActionPayload.DELETE) {
                if (!state.delete(name, privateFrequency, owner)) return;
                clearDeletedSelection(player, handler, stack, portable, name, privateFrequency, false);
                for (long packed : LocalTeleportIndex.get(world).positions()) {
                    BlockPos pos = BlockPos.fromLong(packed);
                    world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    if (world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport
                            && teleport.matchesFrequency(name, privateFrequency, owner)) teleport.removeFrequency();
                }
            } else return;
            TeleportNetworking.syncOpenScreens(world);
        }
    }

    private static void clearDeletedSelection(ServerPlayerEntity player,
            PortableTeleportScreenHandler handler, ItemStack stack, PortableTeleportItem portable,
            String name, boolean privateFrequency, boolean interdimensional) {
        PortableTeleportItem.Selection saved = portable.getSelection(stack);
        if (saved.name().equals(name) && saved.privateFrequency() == privateFrequency
                && saved.interdimensional() == interdimensional)
            saveSelection(player, handler, stack, portable,
                    new PortableTeleportItem.Selection("", privateFrequency, interdimensional));
    }

    static void syncOpenScreens(ServerWorld world, boolean interdimensional) {
        if (interdimensional) {
            for (ServerWorld candidateWorld : world.getServer().getWorlds())
                for (ServerPlayerEntity viewer : candidateWorld.getPlayers())
                    if (viewer.currentScreenHandler instanceof PortableTeleportScreenHandler)
                        sendSnapshot(viewer);
        } else {
            for (ServerPlayerEntity viewer : world.getPlayers())
                if (viewer.currentScreenHandler instanceof PortableTeleportScreenHandler)
                    sendSnapshot(viewer);
        }
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
            teleportInterdimensional(player, sourceWorld, destination, handler, stack, portable,
                    new PortableTeleportItem.Selection(name, payload.privateFrequency(), true));
        } else {
            if (!LocalFrequencyState.get(sourceWorld)
                    .contains(name, payload.privateFrequency(), owner)) return;
            Destination destination = findLocal(player, sourceWorld, player.getBlockPos(),
                    name, payload.privateFrequency(), owner);
            if (destination == null) return;
            teleportLocal(player, sourceWorld, destination, handler, stack, portable,
                    new PortableTeleportItem.Selection(name, payload.privateFrequency(), false));
        }
    }

    /** Returns an action-bar error key, or null after a successful teleport. */
    public static String quickTeleport(ServerPlayerEntity player, ItemStack stack, PortableTeleportItem portable) {
        ServerWorld world = player.getServerWorld();
        PortableTeleportItem.Selection selection = portable.getSelection(stack);
        UUID owner = player.getUuid();
        boolean exists = !selection.name().isEmpty() && (selection.interdimensional()
                ? InterdimensionalFrequencyState.get(world).contains(selection.name(), selection.privateFrequency(), owner)
                : LocalFrequencyState.get(world).contains(selection.name(), selection.privateFrequency(), owner));
        if (!exists) {
            portable.setSelection(stack, new PortableTeleportItem.Selection("",
                    selection.privateFrequency(), selection.interdimensional()));
            return "message.phaseteleporters.portable.no_frequency";
        }
        if (portable.getStoredPE(stack) < TELEPORT_COST)
            return "message.phaseteleporters.portable.no_energy";
        Destination destination = selection.interdimensional()
                ? findInterdimensional(player, world, player.getBlockPos(), selection.name(), selection.privateFrequency(), owner)
                : findLocal(player, world, player.getBlockPos(), selection.name(), selection.privateFrequency(), owner);
        if (destination == null) {
            Destination unpowered = selection.interdimensional()
                    ? findInterdimensional(player, world, player.getBlockPos(), selection.name(),
                            selection.privateFrequency(), owner, false)
                    : findLocal(player, world, player.getBlockPos(), selection.name(),
                            selection.privateFrequency(), owner, false);
            return unpowered != null ? "message.phaseteleporters.portable.no_energy"
                    : "message.phaseteleporters.portable.no_connection";
        }
        boolean success = selection.interdimensional()
                ? teleportInterdimensional(player, world, destination, null, stack, portable, selection)
                : teleportLocal(player, world, destination, null, stack, portable, selection);
        return success ? null : "message.phaseteleporters.portable.no_connection";
    }

    private static Destination findLocal(ServerPlayerEntity player, ServerWorld world, BlockPos from,
            String name, boolean privateFrequency, UUID owner) {
        return findLocal(player, world, from, name, privateFrequency, owner, true);
    }

    private static Destination findLocal(ServerPlayerEntity player, ServerWorld world, BlockPos from,
            String name, boolean privateFrequency, UUID owner, boolean requireEnergy) {
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
                    || !candidate.canWork()
                    || (requireEnergy && candidate.getStored() < REQUIRED_TARGET_ENERGY)
                    || !candidate.canPlayerTeleport(player)) continue;
            long distance = distanceSquared(from, pos);
            if (distance >= bestDistance) continue;
            Vec3d arrival = findSafeArrival(player, world, pos);
            if (arrival == null) continue;
            if (distance < bestDistance) {
                best = new Destination(world, pos, arrival);
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Destination findInterdimensional(ServerPlayerEntity player, ServerWorld sourceWorld,
            BlockPos from, String name, boolean privateFrequency, UUID owner) {
        return findInterdimensional(player, sourceWorld, from, name, privateFrequency, owner, true);
    }

    private static Destination findInterdimensional(ServerPlayerEntity player, ServerWorld sourceWorld,
            BlockPos from, String name, boolean privateFrequency, UUID owner, boolean requireEnergy) {
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
                        || !candidate.canWork()
                        || (requireEnergy && candidate.getStored() < REQUIRED_TARGET_ENERGY)
                        || !candidate.canPlayerTeleport(player)) continue;
                long distance = distanceSquared(from, pos);
                boolean otherDimension = world != sourceWorld;
                boolean bestOtherDimension = best != null && best.world() != sourceWorld;
                if (best != null && ((!otherDimension && bestOtherDimension)
                        || (otherDimension == bestOtherDimension && distance >= bestDistance))) continue;
                Vec3d arrival = findSafeArrival(player, world, pos);
                if (arrival == null) continue;
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

    private static boolean teleportLocal(ServerPlayerEntity player, ServerWorld world, Destination destination,
            PortableTeleportScreenHandler handler, ItemStack stack, PortableTeleportItem portable,
            PortableTeleportItem.Selection selection) {
        if (!(world.getBlockEntity(destination.controller()) instanceof TeleportBlockEntity controller)
                || !controller.canWork()
                || controller.getStored() < REQUIRED_TARGET_ENERGY
                || !controller.canPlayerTeleport(player)
                || portable.getStoredPE(stack) < TELEPORT_COST) return false;
        Vec3d arrival = destination.arrival();
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();
        TeleportStructure.Bounds bounds = controller.getStructureBounds().orElse(null);
        if (bounds != null) PortalReentryGuard.mark(player, world, bounds);
        if (player.teleport(world, arrival.x, arrival.y, arrival.z,
                java.util.Set.of(), player.getYaw(), player.getPitch())) {
            saveSelection(player, handler, stack, portable, selection);
            portable.spendPE(stack, TELEPORT_COST);
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            if (handler != null) player.closeHandledScreen();
            PortalPlaneBlock.playPortableTeleportEffects(world, world,
                    sourceX, sourceY, sourceZ, arrival.x, arrival.y, arrival.z);
            return true;
        } else if (bounds != null) {
            PortalReentryGuard.clear(player);
        }
        return false;
    }

    private static boolean teleportInterdimensional(ServerPlayerEntity player, ServerWorld sourceWorld,
            Destination destination, PortableTeleportScreenHandler handler, ItemStack stack,
            PortableTeleportItem portable, PortableTeleportItem.Selection selection) {
        ServerWorld targetWorld = destination.world();
        if (!(targetWorld.getBlockEntity(destination.controller())
                instanceof InterdimensionalTeleportBlockEntity controller)
                || !controller.canWork()
                || controller.getStored() < REQUIRED_TARGET_ENERGY
                || !controller.canPlayerTeleport(player)
                || portable.getStoredPE(stack) < TELEPORT_COST) return false;
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
            saveSelection(player, handler, stack, portable, selection);
            portable.spendPE(stack, TELEPORT_COST);
            player.getInventory().markDirty();
            player.playerScreenHandler.sendContentUpdates();
            if (bounds != null) InterdimensionalReentryGuard.transferCompleted(player, sourceWorld, targetWorld);
            if (handler != null) player.closeHandledScreen();
            PortalPlaneBlock.playPortableTeleportEffects(sourceWorld, targetWorld,
                    sourceX, sourceY, sourceZ, arrival.x, arrival.y, arrival.z);
            return true;
        } else {
            if (sourceWorld != targetWorld)
                ServerPlayNetworking.send(player, new InterdimensionalTerrainPayload(false));
            if (bounds != null) InterdimensionalReentryGuard.clear(player, "portable-teleport-failed");
        }
        return false;
    }
}
