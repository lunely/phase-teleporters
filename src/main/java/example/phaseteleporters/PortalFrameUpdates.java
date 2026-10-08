package example.phaseteleporters;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Removes a broken frame's portal in the same block update, including corner breaks. */
public final class PortalFrameUpdates {
    private PortalFrameUpdates() {}
    private record Source(BlockPos controller, boolean interdimensional) {}

    private static Source source(BlockEntity entity) {
        if (entity instanceof PortalPlaneBlockEntity plane && plane.controllerPos() != null)
            return new Source(plane.controllerPos(), false);
        if (entity instanceof InterdimensionalPortalPlaneBlockEntity plane && plane.controllerPos() != null)
            return new Source(plane.controllerPos(), true);
        return null;
    }

    public static void frameRemoved(World world, BlockPos framePos) {
        if (world.isClient) return;
        var candidates = new LinkedHashMap<Source, BlockPos>();
        // At a frame corner, the nearest interior plane is diagonal rather than adjacent.
        for (BlockPos candidate : BlockPos.iterate(framePos.add(-1, -1, -1), framePos.add(1, 1, 1))) {
            if (!world.isChunkLoaded(candidate.getX() >> 4, candidate.getZ() >> 4)) continue;
            Source owner = source(world.getBlockEntity(candidate));
            if (owner != null) candidates.putIfAbsent(owner, candidate.toImmutable());
        }
        for (var entry : candidates.entrySet()) {
            Source owner = entry.getKey();
            boolean valid = owner.interdimensional
                    ? InterdimensionalTeleportStructure.isValid(world, owner.controller)
                    : TeleportStructure.isValid(world, owner.controller);
            if (valid) continue;
            BlockEntity controller = world.getBlockEntity(owner.controller);
            if (owner.interdimensional && controller instanceof InterdimensionalTeleportBlockEntity teleport)
                teleport.clearPortal();
            else if (!owner.interdimensional && controller instanceof TeleportBlockEntity teleport)
                teleport.clearPortal();
            // A freshly loaded controller may not yet have cached its active bounds.
            // Remove its connected, owned plane blocks as well, without waiting for a tick.
            clearRemainingPlanes(world, entry.getValue(), owner);
        }
    }

    private static void clearRemainingPlanes(World world, BlockPos start, Source owner) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        var planes = new ArrayList<BlockPos>();
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos) || !world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
                    || !owner.equals(source(world.getBlockEntity(pos)))) continue;
            planes.add(pos);
            for (Direction side : Direction.values()) queue.addLast(pos.offset(side));
        }
        for (BlockPos pos : planes) {
            if (owner.equals(source(world.getBlockEntity(pos))))
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }
}
