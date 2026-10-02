package example.phaseteleporters.energy;

import example.phaseteleporters.PhaseTeleportersMod;
import example.phaseteleporters.BasicEnergyCableBlockEntity;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Connected cables share their stored energy and 500 PE of capacity per cable. */
public final class EnergyCableNetwork {
    private EnergyCableNetwork() {}

    private record Component(List<BasicEnergyCableBlockEntity> cables, Set<BlockPos> positions) {
        long capacity() { return cables.size() * 500L; }
        long stored() {
            long total = 0;
            for (BasicEnergyCableBlockEntity cable : cables) total += cable.localStored();
            return total;
        }
    }

    private static boolean loaded(World world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static Component scan(World world, BlockPos start, BlockPos excluded) {
        List<BasicEnergyCableBlockEntity> cables = new ArrayList<>();
        Set<BlockPos> positions = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(start);
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            if (pos.equals(excluded) || !visited.add(pos) || !loaded(world, pos)
                    || !world.getBlockState(pos).isOf(PhaseTeleportersMod.BASIC_ENERGY_CABLE)) continue;
            if (!(world.getBlockEntity(pos) instanceof BasicEnergyCableBlockEntity cable)) continue;
            positions.add(pos);
            cables.add(cable);
            for (Direction direction : Direction.values()) pending.addLast(pos.offset(direction));
        }
        return new Component(cables, positions);
    }

    public static long stored(World world, BlockPos pos) { return scan(world, pos, null).stored(); }
    public static long capacity(World world, BlockPos pos) { return scan(world, pos, null).capacity(); }

    public static List<BasicEnergyCableBlockEntity> members(World world, BlockPos pos) {
        return scan(world, pos, null).cables();
    }

    private static long remainingTransfer(Component component, long tick, boolean input) {
        long remaining = 0;
        for (BasicEnergyCableBlockEntity cable : component.cables())
            remaining += cable.remainingTransfer(tick, input);
        return remaining;
    }

    private static void consumeTransfer(Component component, long tick, boolean input, long amount) {
        for (BasicEnergyCableBlockEntity cable : component.cables()) {
            long part = Math.min(amount, cable.remainingTransfer(tick, input));
            cable.consumeTransfer(tick, input, part);
            amount -= part;
            if (amount == 0) break;
        }
    }

    public static long insert(World world, BlockPos pos, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        Component component = scan(world, pos, null);
        long accepted = Math.min(Math.min(amount, component.capacity() - component.stored()),
                remainingTransfer(component, world.getTime(), true));
        if (!simulate) {
            consumeTransfer(component, world.getTime(), true, accepted);
            long left = accepted;
            for (BasicEnergyCableBlockEntity cable : component.cables()) {
                long part = Math.min(left, 500 - cable.localStored());
                if (part > 0) cable.setLocalStored(cable.localStored() + part);
                left -= part;
                if (left == 0) break;
            }
        }
        return accepted;
    }

    public static long extract(World world, BlockPos pos, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        Component component = scan(world, pos, null);
        long extracted = Math.min(Math.min(amount, component.stored()),
                remainingTransfer(component, world.getTime(), false));
        if (!simulate) {
            consumeTransfer(component, world.getTime(), false, extracted);
            long left = extracted;
            for (BasicEnergyCableBlockEntity cable : component.cables()) {
                long part = Math.min(left, cable.localStored());
                if (part > 0) cable.setLocalStored(cable.localStored() - part);
                left -= part;
                if (left == 0) break;
            }
        }
        return extracted;
    }

    public static void onCableRemoved(World world, BlockPos removedPos, BasicEnergyCableBlockEntity removed) {
        if (world.isClient) return;
        long total = removed.localStored();
        List<Component> remaining = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = removedPos.offset(direction);
            if (seen.contains(neighbor)) continue;
            Component component = scan(world, neighbor, removedPos);
            if (component.cables().isEmpty()) continue;
            seen.addAll(component.positions());
            total += component.stored();
            remaining.add(component);
        }
        for (Component component : remaining) {
            for (BasicEnergyCableBlockEntity cable : component.cables()) cable.setLocalStored(0);
            for (BasicEnergyCableBlockEntity cable : component.cables()) {
                long part = Math.min(total, 500);
                cable.setLocalStored(part);
                total -= part;
                if (total == 0) break;
            }
        }
        removed.setLocalStored(0);
    }

    private static boolean canOutput(PEStorage source, Direction side) {
        return !(source instanceof PEBlockEntity block)
                || (block.canWork() && block.getSideMode(side).allowsOutput());
    }

    private static boolean canInput(PEStorage receiver, Direction side) {
        return !(receiver instanceof PEBlockEntity block) || block.getSideMode(side).allowsInput();
    }

    private static long transfer(PEStorage source, PEStorage receiver,
            Direction sourceSide, Direction receiverSide) {
        if (source == receiver) return 0;
        if (!canOutput(source, sourceSide) || !canInput(receiver, receiverSide)) return 0;
        return source.transferTo(receiver, Long.MAX_VALUE);
    }

    private record Route(BlockPos position, Direction sourceSide, Component network) {}

    private static long transferThroughNetwork(World world, Component component,
            PEStorage source, PEStorage receiver, Direction sourceSide, Direction receiverSide) {
        if (source == receiver || !canOutput(source, sourceSide) || !canInput(receiver, receiverSide)) return 0;
        long tick = world.getTime();
        long limit = Math.min(remainingTransfer(component, tick, true),
                remainingTransfer(component, tick, false));
        long sent = source.transferTo(receiver, limit);
        // Flow can pass through without fitting in the small persistent cable buffer.
        consumeTransfer(component, tick, true, sent);
        consumeTransfer(component, tick, false, sent);
        return sent;
    }

    private static boolean hasAcceptingReceiver(World world, Component component, PEStorage source) {
        for (BlockPos cablePos : component.positions()) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = cablePos.offset(direction);
                if (component.positions().contains(neighbor) || !loaded(world, neighbor)) continue;
                if (world.getBlockEntity(neighbor) instanceof PEStorage receiver
                        && receiver != source && canInput(receiver, direction.getOpposite())
                        && receiver.insert(1, true) > 0) return true;
                if (!(world.getBlockEntity(neighbor) instanceof PEStorage)
                        && EnergyApiCompat.accepts(world, neighbor, direction.getOpposite())) return true;
            }
        }
        return false;
    }

    /** Consumers receive stored and incoming energy before any surplus is buffered. */
    public static void distribute(World world, BlockPos sourcePos, PEStorage source) {
        ArrayDeque<Route> pending = new ArrayDeque<>();
        Set<BlockPos> handledCables = new HashSet<>();
        List<Route> networks = new ArrayList<>();
        pending.add(new Route(sourcePos, null, null));
        while (!pending.isEmpty()) {
            Route route = pending.removeFirst();
            BlockPos current = route.position();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.offset(direction);
                Direction sourceSide = route.sourceSide() == null ? direction : route.sourceSide();
                if (neighbor.equals(sourcePos) || !loaded(world, neighbor)
                        || !canOutput(source, sourceSide)) continue;
                if (world.getBlockState(neighbor).isOf(PhaseTeleportersMod.BASIC_ENERGY_CABLE)) {
                    if (handledCables.contains(neighbor)) continue;
                    Component component = scan(world, neighbor, null);
                    handledCables.addAll(component.positions());
                    if (!component.cables().isEmpty()) {
                        networks.add(new Route(neighbor, sourceSide, component));
                        for (BlockPos cablePos : component.positions())
                            pending.addLast(new Route(cablePos, sourceSide, component));
                    }
                } else {
                    BlockEntity blockEntity = world.getBlockEntity(neighbor);
                    if (blockEntity instanceof PEStorage receiver) {
                        if (route.network() == null)
                            transfer(source, receiver, sourceSide, direction.getOpposite());
                        else {
                            transfer(route.network().cables().getFirst(), receiver,
                                    direction, direction.getOpposite());
                            transferThroughNetwork(world, route.network(), source, receiver,
                                    sourceSide, direction.getOpposite());
                        }
                    } else if (route.network() == null) {
                        EnergyApiCompat.push(world, neighbor, direction.getOpposite(), source, Long.MAX_VALUE);
                    } else {
                        PEStorage buffer = route.network().cables().getFirst();
                        EnergyApiCompat.push(world, neighbor, direction.getOpposite(), buffer, Long.MAX_VALUE);
                        long tick = world.getTime();
                        long limit = Math.min(remainingTransfer(route.network(), tick, true),
                                remainingTransfer(route.network(), tick, false));
                        long sent = EnergyApiCompat.push(world, neighbor, direction.getOpposite(), source, limit);
                        consumeTransfer(route.network(), tick, true, sent);
                        consumeTransfer(route.network(), tick, false, sent);
                    }
                }
            }
        }
        for (Route network : networks) {
            if (!hasAcceptingReceiver(world, network.network(), source))
                transfer(source, network.network().cables().getFirst(), network.sourceSide(), null);
        }
    }

    /** Stored PE continues flowing to consumers even after a source is removed. */
    public static void distributeStored(World world, BasicEnergyCableBlockEntity cable) {
        Component component = scan(world, cable.getPos(), null);
        long tick = world.getTime();
        if (component.cables().isEmpty() || cable.processedTick() == tick) return;
        for (BasicEnergyCableBlockEntity member : component.cables()) member.setProcessedTick(tick);
        Set<BlockPos> receivers = new HashSet<>();
        for (BasicEnergyCableBlockEntity member : component.cables()) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = member.getPos().offset(direction);
                if (component.positions().contains(neighbor) || !loaded(world, neighbor)
                        || receivers.contains(neighbor)) continue;
                BlockEntity blockEntity = world.getBlockEntity(neighbor);
                if (blockEntity instanceof PEStorage receiver) {
                    if (transfer(cable, receiver, direction, direction.getOpposite()) > 0)
                        receivers.add(neighbor);
                } else if (EnergyApiCompat.push(world, neighbor, direction.getOpposite(),
                        cable, Long.MAX_VALUE) > 0) {
                    receivers.add(neighbor);
                }
            }
        }
    }
}
