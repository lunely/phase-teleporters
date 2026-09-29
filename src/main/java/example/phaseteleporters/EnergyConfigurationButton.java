package example.phaseteleporters;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class EnergyConfigurationButton {
    private EnergyConfigurationButton() {}

    public static ButtonWidget create(int x, int y, ScreenHandler handler) {
        return ButtonWidget.builder(Text.literal("::"),
                button -> {
                    var interaction = MinecraftClient.getInstance().interactionManager;
                    if (interaction != null)
                        interaction.clickButton(handler.syncId, EnergyConfigurationScreenHandler.OPEN_BUTTON);
                })
                .dimensions(x, y, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("gui.phaseteleporters.configure_sides")))
                .build();
    }
}
