package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Draws the empty and filled sprites in the energy slot. */
public final class EnergyBarRenderer {
    private static final Identifier EMPTY =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILL =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/electric_furnace_energy_filled.png");
    private static final int WIDTH = 16;
    private static final int HEIGHT = 48;

    private EnergyBarRenderer() {}

    public static void draw(DrawContext context, int x, int y, long current, long capacity) {
        int filled = capacity <= 0 || current <= 0 ? 0
                : (int) (HEIGHT * ((double) Math.min(current, capacity) / capacity));
        int emptyHeight = HEIGHT - filled;
        if (emptyHeight > 0) {
            context.drawTexture(EMPTY, x, y, 0, 0, WIDTH, emptyHeight, WIDTH, HEIGHT);
        }
        if (filled > 0) {
            context.drawTexture(FILL, x, y + emptyHeight, 0, emptyHeight, WIDTH, filled, WIDTH, HEIGHT);
        }
    }

    public static boolean contains(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
    }
}
