package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class CreativeEnergyCubeScreen extends HandledScreen<CreativeEnergyCubeScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();

    public CreativeEnergyCubeScreen(CreativeEnergyCubeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
        playerInventoryTitleY = 72;
    }

    @Override protected void init() {
        super.init();
        titleX = backgroundWidth - 8 - textRenderer.getWidth(title);
        playerInventoryTitleX = backgroundWidth - 8
                - textRenderer.getWidth(Text.translatable("container.inventory"));
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        EnergyCubeGuiRenderer.draw(context, x, y, 0, 0, true);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (EnergyCubeGuiRenderer.overBar(x, y, mouseX, mouseY)) {
            context.drawTooltip(textRenderer, Text.literal("∞ J"), mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)
                || super.mouseClicked(mouseX, mouseY, button);
    }
}
