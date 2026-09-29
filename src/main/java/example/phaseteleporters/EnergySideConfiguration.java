package example.phaseteleporters;

import example.phaseteleporters.energy.PEBlockEntity;
import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PERedstoneMode;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/** Synchronizes the six PE faces in the machine screen that owns the block. */
public final class EnergySideConfiguration {
    public static final int FIRST_BUTTON = 100;
    public static final int REDSTONE_FIRST_BUTTON = FIRST_BUTTON + 6;
    public static final int SECURITY_FIRST_BUTTON = REDSTONE_FIRST_BUTTON + 3;
    public static final int LAST_BUTTON_EXCLUSIVE = SECURITY_FIRST_BUTTON + 2;

    private EnergySideConfiguration() {}

    public static PropertyDelegate properties(PEBlockEntity block) {
        return properties(block, null);
    }

    public static PropertyDelegate properties(PEBlockEntity block, PlayerEntity viewer) {
        if (block == null) return new ArrayPropertyDelegate(11);
        return new PropertyDelegate() {
            @Override public int get(int index) {
                return switch (index) {
                    case 6 -> facing(block).getId();
                    case 7 -> block.getRedstoneMode().ordinal();
                    case 8 -> block.hasRedstoneSignal() ? 1 : 0;
                    case 9 -> block.isPublicAccess() ? 1 : 0;
                    case 10 -> viewer != null && block.isOwner(viewer) ? 1 : 0;
                    default -> block.getSideMode(Direction.byId(index)).ordinal();
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int size() { return 11; }
        };
    }

    public static Direction facing(PropertyDelegate properties) {
        return Direction.byId(properties.get(6));
    }

    private static Direction facing(PEBlockEntity block) {
        var state = block.getCachedState();
        return state.contains(Properties.HORIZONTAL_FACING)
                ? state.get(Properties.HORIZONTAL_FACING) : Direction.DOWN;
    }

    /** GUI cells are ordered top, left, front, right, bottom, back. */
    public static Direction sideFor(Direction front, int cell) {
        if (!front.getAxis().isHorizontal()) {
            return switch (cell) {
                case 0 -> Direction.UP;
                case 1 -> Direction.WEST;
                case 2 -> Direction.NORTH;
                case 3 -> Direction.EAST;
                case 4 -> Direction.DOWN;
                case 5 -> Direction.SOUTH;
                default -> throw new IllegalArgumentException("Invalid side cell: " + cell);
            };
        }
        return switch (cell) {
            case 0 -> Direction.UP;
            case 1 -> front.rotateYClockwise();
            case 2 -> front;
            case 3 -> front.rotateYCounterclockwise();
            case 4 -> Direction.DOWN;
            case 5 -> front.getOpposite();
            default -> throw new IllegalArgumentException("Invalid side cell: " + cell);
        };
    }

    public static PESideMode mode(PropertyDelegate properties, Direction side) {
        return PESideMode.byId(properties.get(side.getId()));
    }

    public static PERedstoneMode redstoneMode(PropertyDelegate properties) {
        return PERedstoneMode.byId(properties.get(7));
    }

    public static boolean hasRedstoneSignal(PropertyDelegate properties) {
        return properties.get(8) != 0;
    }

    public static boolean isPublicAccess(PropertyDelegate properties) {
        return properties.get(9) != 0;
    }

    public static boolean isSecurityOwner(PropertyDelegate properties) {
        return properties.get(10) != 0;
    }

    public static boolean click(PlayerEntity player, int buttonId, PEBlockEntity block) {
        if (buttonId < FIRST_BUTTON || buttonId >= LAST_BUTTON_EXCLUSIVE || block == null) return false;
        var pos = block.getPos();
        if (player.getWorld().getBlockEntity(pos) != block
                || player.squaredDistanceTo(pos.toCenterPos()) > 64.0
                || !block.canPlayerUse(player)) return false;
        if (buttonId >= SECURITY_FIRST_BUTTON) {
            if (!block.isOwner(player)) return false;
            block.setPublicAccess(buttonId == SECURITY_FIRST_BUTTON);
        } else if (buttonId >= REDSTONE_FIRST_BUTTON)
            block.setRedstoneMode(PERedstoneMode.byId(buttonId - REDSTONE_FIRST_BUTTON));
        else block.cycleSideMode(sideFor(facing(block), buttonId - FIRST_BUTTON));
        return true;
    }
}
