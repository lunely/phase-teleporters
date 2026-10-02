package example.phaseteleporters;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Shared read-only energy gauge for machine screens. Coordinates are screen-space. */
public final class EnergyBarRenderer {
    private static final int WIDTH = 16;
    private static final int HEIGHT = 56;
    private static final int INNER_HEIGHT = HEIGHT - 4;

    private EnergyBarRenderer() {}

    public static void draw(DrawContext context, int x, int y, long current, long capacity) {
        context.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF30343A);
        context.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, 0xFF343A3F);
        if (current <= 0 || capacity <= 0) return;
        int filled = Math.clamp((int) Math.ceil(INNER_HEIGHT * (double) current / capacity), 1, INNER_HEIGHT);
        context.fill(x + 2, y + HEIGHT - 2 - filled, x + WIDTH - 2, y + HEIGHT - 2, 0xFF56C7E5);
    }

    public static void drawTooltip(DrawContext context, TextRenderer renderer, int x, int y,
            int mouseX, int mouseY, long current, long capacity) {
        if (mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT)
            context.drawTooltip(renderer, Text.literal(PEGuiText.teleporterEnergy(current, capacity)), mouseX, mouseY);
    }
}
