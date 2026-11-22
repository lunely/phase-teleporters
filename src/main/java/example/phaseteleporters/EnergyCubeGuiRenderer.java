package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Shared visual layout for the normal and creative Energy Cube screens. */
final class EnergyCubeGuiRenderer {
    private static final Identifier BACKGROUND =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/energy_cube.png");
    private static final Identifier INPUT =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/energy_input.png");
    private static final Identifier OUTPUT =
            Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/energy_output.png");

    static final int BAR_X = 80;
    static final int BAR_Y = 17;

    private EnergyCubeGuiRenderer() {}

    static void draw(DrawContext context, int x, int y, long energy, long capacity,
                     boolean creative, boolean inputOccupied, boolean outputOccupied) {
        context.drawTexture(BACKGROUND, x, y, 0, 0, 176, 166, 256, 256);
        EnergyBarRenderer.draw(context, x + BAR_X, y + BAR_Y, creative ? 1 : energy, creative ? 1 : capacity);
        if (!inputOccupied) {
            context.drawTexture(INPUT, x + 51, y + 31, 0, 0, 18, 18, 18, 18);
        }
        if (!outputOccupied) {
            context.drawTexture(OUTPUT, x + 107, y + 31, 0, 0, 18, 18, 18, 18);
        }
    }

    static boolean overBar(int x, int y, int mouseX, int mouseY) {
        return EnergyBarRenderer.contains(x + BAR_X, y + BAR_Y, mouseX, mouseY);
    }
}
