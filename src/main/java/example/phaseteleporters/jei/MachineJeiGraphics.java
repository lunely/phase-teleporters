package example.phaseteleporters.jei;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

final class MachineJeiGraphics {
    private static final Identifier ENERGY_SLOT =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");
    private static final Identifier EMPTY_ENERGY =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_filled.png");

    private MachineJeiGraphics() {}

    static void drawMachineBackground(DrawContext context, Identifier texture) {
        context.drawTexture(texture, 0, 0, 0, 0, 176, 83);
        context.fill(7, 7, 25, 57, 0xFF373737);
        context.drawTexture(EMPTY_ENERGY, 8, 8, 0, 0, 16, 48, 16, 48);
        context.drawTexture(FILLED_ENERGY, 8, 8, 0, 0, 16, 48, 16, 48);
        context.drawTexture(ENERGY_SLOT, 8, 62, 0, 0, 16, 16, 16, 16);
    }
}
