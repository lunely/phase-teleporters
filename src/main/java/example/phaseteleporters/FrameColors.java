package example.phaseteleporters;

import java.util.HashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.item.ItemStack;

/** Stable color IDs mapped to the hand-editable vertical frame LED atlas. */
public final class FrameColors {
    public static final int DEFAULT = 9;
    private static final int[] ATLAS_COLOR_IDS = {
            13, 11, 15, 2, 1, 16, 7, 4, 9, 10, 0, 14, 17, 5, 6, 18, 3, 8, 12, 19
    };

    private FrameColors() {}

    public static int count() { return ATLAS_COLOR_IDS.length; }

    public static int atlasRow(int color) {
        for (int row = 0; row < ATLAS_COLOR_IDS.length; row++) {
            if (ATLAS_COLOR_IDS[row] == color) return row;
        }
        return atlasRow(DEFAULT);
    }

    public static int itemColor(ItemStack stack) {
        return itemColor(stack, DEFAULT);
    }

    public static int itemColor(ItemStack stack, int defaultColor) {
        BlockStateComponent component = stack.get(DataComponentTypes.BLOCK_STATE);
        Integer color = component == null ? null : component.getValue(TeleportationFrameBlock.COLOR);
        return color != null && PortalColors.isValid(color) ? color : defaultColor;
    }

    public static void setItemColor(ItemStack stack, int color) {
        setItemColor(stack, color, DEFAULT);
    }

    public static void setItemColor(ItemStack stack, int color, int defaultColor) {
        BlockStateComponent component = stack.getOrDefault(
                DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT);
        int selected = PortalColors.isValid(color) ? color : defaultColor;
        if (selected == defaultColor) {
            // Default-colored drops stack with freshly crafted, uncolored frame items.
            var properties = new HashMap<>(component.properties());
            properties.remove(TeleportationFrameBlock.COLOR.getName());
            if (properties.isEmpty()) stack.remove(DataComponentTypes.BLOCK_STATE);
            else stack.set(DataComponentTypes.BLOCK_STATE, new BlockStateComponent(properties));
        } else {
            stack.set(DataComponentTypes.BLOCK_STATE, component.with(TeleportationFrameBlock.COLOR, selected));
        }
    }
}
