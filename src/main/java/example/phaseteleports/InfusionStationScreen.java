package example.phaseteleports;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class InfusionStationScreen extends HandledScreen<InfusionStationScreenHandler> {
    private ButtonWidget clearButton;

    public InfusionStationScreen(InfusionStationScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 178;
        playerInventoryTitleY = 84;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        addDrawableChild(EnergyConfigurationButton.create(x - 24, y + 6, handler));
        clearButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleports.clear"), button -> {
                    if (client != null && client.interactionManager != null) {
                        client.interactionManager.clickButton(handler.syncId, 0);
                    }
                }).dimensions(x + 96, y + 64, 72, 17).build());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF30343A);
        context.fill(x + 4, y + 4, x + 172, y + 174, 0xFFBFC0C0);
        context.fill(x + 6, y + 6, x + 170, y + 92, 0xFFADB0B2);
        drawSlot(context, x + 43, y + 34);
        drawSlot(context, x + 79, y + 34);
        drawSlot(context, x + 115, y + 34);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) drawSlot(context, x + 7 + col * 18, y + 95 + row * 18);
        }
        for (int col = 0; col < 9; col++) drawSlot(context, x + 7 + col * 18, y + 153);
        context.fill(x + 14, y + 20, x + 30, y + 64, 0xFF25282C);
        context.fill(x + 16, y + 22, x + 28, y + 62, InfusionResource.NONE.barColor());
        int amount = handler.getInfusionAmount();
        if (amount > 0) {
            int fillHeight = Math.max(1, 40 * amount / InfusionStationBlockEntity.MAX_INFUSION);
            context.fill(x + 16, y + 62 - fillHeight, x + 28, y + 62,
                    handler.getInfusionResource().barColor());
        }
        context.fill(x + 103, y + 40, x + 111, y + 47, 0xFF55575A);
        context.fill(x + 108, y + 37, x + 114, y + 50, 0xFF55575A);
        int progress = handler.getProcessTime() == 0 ? 0
                : 16 * handler.getProgress() / handler.getProcessTime();
        context.fill(x + 99, y + 53, x + 99 + progress, y + 56, 0xFFC84539);
        EnergyBarRenderer.draw(context, x - 20, y + 20, handler.getEnergy(), handler.getMaxEnergy());
    }

    private static void drawSlot(DrawContext context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, 0xFF55575A);
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFF25282C);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        super.drawForeground(context, mouseX, mouseY);
        drawCenteredLabel(context, "gui.phaseteleports.infusion", 52);
        drawCenteredLabel(context, "gui.phaseteleports.base", 88);
        drawCenteredLabel(context, "gui.phaseteleports.output", 124);
        context.drawText(textRenderer,
                handler.getInfusionAmount() + " / " + InfusionStationBlockEntity.MAX_INFUSION,
                8, 72, 0xFF303030, false);
    }

    private void drawCenteredLabel(DrawContext context, String key, int centerX) {
        Text label = Text.translatable(key);
        context.drawText(textRenderer, label, centerX - textRenderer.getWidth(label) / 2, 56, 0xFF303030, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        clearButton.active = handler.getProgress() == 0 && handler.getInfusionAmount() > 0;
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        EnergyBarRenderer.drawTooltip(context, textRenderer, (width - backgroundWidth) / 2 - 20,
                (height - backgroundHeight) / 2 + 20, mouseX, mouseY,
                handler.getEnergy(), handler.getMaxEnergy());
    }
}
