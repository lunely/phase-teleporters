package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SolarPanelScreen extends HandledScreen<SolarPanelScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();
    private static final Identifier BACKGROUND = texture("solar_panel");
    private static final Identifier ENERGY_SLOT = texture("slot/energy_output");
    private static final int ENERGY_X = 8, ENERGY_Y = 8;
    private static Identifier texture(String name) { return Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/" + name + ".png"); }
    public SolarPanelScreen(SolarPanelScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176; backgroundHeight = 166; playerInventoryTitleY = 72;
    }
    @Override protected void init() {
        super.init();
        titleX = backgroundWidth - 8 - textRenderer.getWidth(title);
        playerInventoryTitleX = backgroundWidth - 8 - textRenderer.getWidth(Text.translatable("container.inventory"));
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(BACKGROUND, x, y, 0, 0, 176, 166, 256, 256);
        EnergyBarRenderer.draw(context, x + ENERGY_X, y + ENERGY_Y, handler.energy(), handler.capacity());
        if (!handler.getSlot(0).hasStack()) {
            context.drawTexture(ENERGY_SLOT, x + 7, y + 61, 0, 0, 18, 18, 18, 18);
        }
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (EnergyBarRenderer.contains(x + ENERGY_X, y + ENERGY_Y, mouseX, mouseY)) {
            context.drawTooltip(textRenderer, java.util.List.of(
                    Text.translatable("gui.phaseteleporters.solar_panel.generation_rate", handler.generation())
                            .formatted(net.minecraft.util.Formatting.GREEN),
                    Text.literal(PEGuiText.energy(handler.energy(), handler.capacity()))), mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)
                || super.mouseClicked(mouseX, mouseY, button);
    }
}
