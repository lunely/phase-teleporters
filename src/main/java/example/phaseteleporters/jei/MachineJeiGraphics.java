package example.phaseteleporters.jei;

import example.phaseteleporters.EnergyBarRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

final class MachineJeiGraphics {
    private static final Identifier ENERGY_SLOT =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");

    private MachineJeiGraphics() {}

    static void drawMachineBackground(DrawContext context, Identifier texture) {
        context.drawTexture(texture, 0, 0, 0, 0, 176, 83);
        EnergyBarRenderer.draw(context, 8, 8, 1, 1);
        context.drawTexture(ENERGY_SLOT, 8, 62, 0, 0, 16, 16, 16, 16);
    }
}
