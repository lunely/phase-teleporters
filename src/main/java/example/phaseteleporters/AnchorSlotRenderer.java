package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;

/** A subdued module silhouette shows what belongs in an empty anchor slot. */
public final class AnchorSlotRenderer {
    private AnchorSlotRenderer() {}

    public static void draw(DrawContext context, int x, int y, boolean occupied) {
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF454545);
        context.fill(x, y, x + 16, y + 16, 0xFF9A9A9A);
        if (occupied) return;
        context.fill(x + 4, y + 3, x + 12, y + 12, 0x66526072);
        context.fill(x + 6, y + 5, x + 10, y + 10, 0x6678D8B7);
        context.fill(x + 5, y + 12, x + 7, y + 15, 0x668C826A);
        context.fill(x + 9, y + 12, x + 11, y + 15, 0x668C826A);
    }
}
