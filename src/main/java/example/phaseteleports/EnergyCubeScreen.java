package example.phaseteleports;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class EnergyCubeScreen extends HandledScreen<EnergyCubeScreenHandler> {
    public EnergyCubeScreen(EnergyCubeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 195;
    }

    @Override protected void init() {
        super.init();
        addDrawableChild(EnergyConfigurationButton.create(
                (width - backgroundWidth) / 2 - 24, (height - backgroundHeight) / 2 + 6, handler));
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.fill(x, y, x + 176, y + 195, 0xFF30343A);
        context.fill(x + 4, y + 4, x + 172, y + 191, 0xFFBFC0C0);
        context.fill(x + 12, y + 28, x + 164, y + 62, 0xFF25282C);
        long capacity = handler.getMaxEnergy();
        int filled = capacity <= 0 ? 0 : (int) (148 * handler.getEnergy() / capacity);
        context.fill(x + 14, y + 30, x + 14 + filled, y + 60, 0xFF5ACD70);
        drawSlot(context, x + 52, y + 78);
        drawSlot(context, x + 105, y + 78);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawSlot(context, x + 8 + col * 18, y + 112 + row * 18);
        for (int col = 0; col < 9; col++) drawSlot(context, x + 8 + col * 18, y + 170);
    }

    private void drawSlot(DrawContext context, int x, int y) {
        context.fill(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
        context.fill(x, y, x + 16, y + 16, 0xFF9F9F9F);
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 9, 0xFF303030, false);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(handler.getEnergy() + " / " + handler.getMaxEnergy() + " PE"),
                88, 40, 0xFFFFFFFF);
        context.drawText(textRenderer, Text.translatable("gui.phaseteleports.charge"), 39, 68, 0xFF303030, false);
        context.drawText(textRenderer, Text.translatable("gui.phaseteleports.discharge"), 89, 68, 0xFF303030, false);
        context.drawText(textRenderer, Text.translatable("container.inventory"), 8, 101, 0xFF303030, false);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
