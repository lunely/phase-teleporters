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

    public static long insert(World world, BlockPos pos, long amount, boolean simulate) {
        if (amount <= 0) return 0;
        Component component = scan(world, pos, null);
        long accepted = Math.min(amount, component.capacity() - component.stored());
        if (!simulate) {
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
        long extracted = Math.min(amount, component.stored());
        if (!simulate) {
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

    private record Route(BlockPos position, Direction sourceSide) {}

    /** Sources fill connected cable storage and every receiver reachable through it. */
    public static void distribute(World world, BlockPos sourcePos, PEStorage source) {
        ArrayDeque<Route> pending = new ArrayDeque<>();
        Set<BlockPos> handledCables = new HashSet<>();
        pending.add(new Route(sourcePos, null));
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
                        transfer(source, component.cables().getFirst(), sourceSide, direction.getOpposite());
                        for (BlockPos cablePos : component.positions())
                            pending.addLast(new Route(cablePos, sourceSide));
                    }
                } else {
                    BlockEntity blockEntity = world.getBlockEntity(neighbor);
                    if (blockEntity instanceof PEStorage receiver)
                        transfer(source, receiver, sourceSide, direction.getOpposite());
                }
            }
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
                }
            }
        }
    }
}
