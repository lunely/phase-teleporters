package example.phaseteleporters;

import java.util.Optional;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Finds an upright, closed frame with Teleport at the center of its bottom edge. */
public final class TeleportStructure {
    private TeleportStructure() {}

    public record Bounds(Direction.Axis axis, BlockPos min, BlockPos max, int width, int height) {
        public BlockPos interiorMin() { return min.add(axis == Direction.Axis.X ? 1 : 0, 1,
                axis == Direction.Axis.Z ? 1 : 0); }
        public BlockPos interiorMax() { return max.add(axis == Direction.Axis.X ? -1 : 0, -1,
                axis == Direction.Axis.Z ? -1 : 0); }
        public boolean containsInterior(BlockPos pos) {
            BlockPos low = interiorMin();
            BlockPos high = interiorMax();
            return pos.getX() >= low.getX() && pos.getX() <= high.getX()
                    && pos.getY() >= low.getY() && pos.getY() <= high.getY()
                    && pos.getZ() >= low.getZ() && pos.getZ() <= high.getZ();
        }
    }

    public static boolean isValid(World world, BlockPos teleportPos) {
        return find(world, teleportPos).isPresent();
    }

    public static Optional<Bounds> find(World world, BlockPos teleportPos) {
        if (!world.getBlockState(teleportPos).isOf(PhaseTeleportersMod.TELEPORT)) return Optional.empty();
        Optional<Bounds> alongX = findAlong(world, teleportPos, Direction.Axis.X);
        return alongX.isPresent() ? alongX : findAlong(world, teleportPos, Direction.Axis.Z);
    }

    private static Optional<Bounds> findAlong(World world, BlockPos base, Direction.Axis axis) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        int left = countBottomFrames(world, base, -dx, -dz);
        int right = countBottomFrames(world, base, dx, dz);
        // The Teleport must be the single center block, so both sides have equal spans.
        for (int radius = 1; radius <= Math.min(left, right); radius++) {
            for (int y = 1; base.getY() + y < world.getTopY(); y++) {
                BlockPos leftSide = base.add(-radius * dx, y, -radius * dz);
                BlockPos rightSide = base.add(radius * dx, y, radius * dz);
                if (!isFrame(world, leftSide) || !isFrame(world, rightSide)) break;

                boolean allClear = true;
                boolean allFrame = true;
                for (int offset = -radius + 1; offset < radius; offset++) {
                    BlockPos inside = base.add(offset * dx, y, offset * dz);
                    var state = world.getBlockState(inside);
                    allClear &= state.isAir() || (state.isOf(PhaseTeleportersMod.PORTAL_PLANE)
                            && world.getBlockEntity(inside) instanceof PortalPlaneBlockEntity plane
                            && plane.belongsTo(base));
                    allFrame &= state.isOf(PhaseTeleportersMod.TELEPORTATION_FRAME);
                }
                if (y >= 3 && allFrame) {
                    return Optional.of(bounds(base, axis, radius, radius, y + 1));
                }
                if (!allClear) break;
            }
        }
        return Optional.empty();
    }

    private static int countBottomFrames(World world, BlockPos base, int dx, int dz) {
        int count = 0;
        while (isFrame(world, base.add((count + 1) * dx, 0, (count + 1) * dz))) count++;
        return count;
    }

    private static boolean isFrame(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(PhaseTeleportersMod.TELEPORTATION_FRAME);
    }

    public static Bounds bounds(BlockPos base, Direction.Axis axis, int leftSpan, int rightSpan, int height) {
        int dx = axis == Direction.Axis.X ? 1 : 0;
        int dz = axis == Direction.Axis.Z ? 1 : 0;
        return new Bounds(axis, base.add(-leftSpan * dx, 0, -leftSpan * dz),
                base.add(rightSpan * dx, height - 1, rightSpan * dz),
                leftSpan + rightSpan + 1, height);
    }
}
