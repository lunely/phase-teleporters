package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class EnrichmentChamberScreen extends HandledScreen<EnrichmentChamberScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();
    private static final Identifier GUI_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/enrichment_chamber.png");
    private static final Identifier ENERGY_SLOT_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 8;

    public EnrichmentChamberScreen(EnrichmentChamberScreenHandler handler,
            PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
        playerInventoryTitleY = 72;
    }

    @Override protected void init() {
        super.init();
        titleX = backgroundWidth - 8 - textRenderer.getWidth(title);
        playerInventoryTitleX = backgroundWidth - 8 - textRenderer.getWidth(Text.translatable("container.inventory"));
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
        drawEnergy(context);
        if (!handler.getSlot(2).hasStack()) {
            context.drawTexture(ENERGY_SLOT_TEXTURE, x + 8, y + 62, 0, 0, 16, 16, 16, 16);
        }

        int processTime = handler.getProcessTime();
        if (processTime > 0) {
            int progress = Math.min(24, 24 * handler.getProgress() / processTime);
            if (progress > 0) {
                context.drawTexture(GUI_TEXTURE, x + 90, y + 32, 178, 0,
                        progress, 17, 256, 256);
            }
        }
    }

    private void drawEnergy(DrawContext context) {
        EnergyBarRenderer.draw(context, x + ENERGY_X, y + ENERGY_Y, handler.getEnergy(), handler.getMaxEnergy());
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (EnergyBarRenderer.contains(x + ENERGY_X, y + ENERGY_Y, mouseX, mouseY)) {
            context.drawTooltip(textRenderer,
                    Text.literal(PEGuiText.energy(handler.getEnergy(), handler.getMaxEnergy())),
                    mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)
                || super.mouseClicked(mouseX, mouseY, button);
    }
}
