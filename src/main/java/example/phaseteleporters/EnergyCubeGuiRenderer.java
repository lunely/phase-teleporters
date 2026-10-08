package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Shared visual layout for the normal and creative Energy Cube screens. */
final class EnergyCubeGuiRenderer {
    private static final Identifier BACKGROUND =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/energy_cube.png");
    private static final Identifier EMPTY =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/electric_furnace_energy_filled.png");
    private static final Identifier INPUT =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/energy_input.png");
    private static final Identifier OUTPUT =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/energy_output.png");

    static final int BAR_X = 80;
    static final int BAR_Y = 17;
    static final int BAR_WIDTH = 16;
    static final int BAR_HEIGHT = 48;

    private EnergyCubeGuiRenderer() {}

    static void draw(DrawContext context, int x, int y, long energy, long capacity,
                     boolean creative, boolean inputOccupied, boolean outputOccupied) {
        context.drawTexture(BACKGROUND, x, y, 0, 0, 176, 166, 256, 256);
        int left = x + BAR_X;
        int top = y + BAR_Y;
        int bottom = top + BAR_HEIGHT;
        // Match the even one-pixel rim used by the other machine energy bars.
        context.fill(left - 1, top - 1, left + BAR_WIDTH + 1, bottom + 1, 0xFF373737);
        context.drawTexture(EMPTY, left, top, 0, 0,
                BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
        int filled = creative ? BAR_HEIGHT : capacity <= 0 ? 0
                : (int) Math.clamp((double) energy * BAR_HEIGHT / capacity, 0, BAR_HEIGHT);
        if (filled > 0) {
            context.drawTexture(FILLED, left, bottom - filled,
                    0, BAR_HEIGHT - filled, BAR_WIDTH, filled, BAR_WIDTH, BAR_HEIGHT);
        }
        if (!inputOccupied) {
            context.drawTexture(INPUT, x + 51, y + 31, 0, 0, 18, 18, 18, 18);
        }
        if (!outputOccupied) {
            context.drawTexture(OUTPUT, x + 107, y + 31, 0, 0, 18, 18, 18, 18);
        }
    }

    static boolean overBar(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x + BAR_X && mouseX < x + BAR_X + BAR_WIDTH
                && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_HEIGHT;
    }
}
