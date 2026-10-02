package example.phaseteleporters;

import example.phaseteleporters.energy.PESideMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class EnergyConfigurationScreen extends HandledScreen<EnergyConfigurationScreenHandler> {
    private static final int SIZE = 44;
    private static final int[] CELLS = {0, 4, 1, 3, 2, 5};
    private static final int[][] POSITIONS = {
            {102, 25}, {102, 169}, {54, 73},
            {150, 73}, {102, 73}, {102, 121}
    };
    private static final String[] SIDE_KEYS = {
            "top", "bottom", "left", "right", "front", "back"
    };

    public EnergyConfigurationScreen(EnergyConfigurationScreenHandler handler,
            PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 248;
        backgroundHeight = 232;
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF30343A);
        context.fill(x + 3, y + 3, x + backgroundWidth - 3, y + backgroundHeight - 3, 0xFFC6C6C6);
        context.fill(x + 5, y + 5, x + backgroundWidth - 5, y + backgroundHeight - 5, 0xFFD4D4D4);
        context.fill(x + 222, y + 5, x + 244, y + 25, 0xFF555555);
        context.fill(x + 224, y + 7, x + 242, y + 23, 0xFFB9B9B9);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("×"), x + 233, y + 10, 0xFF333333);
        for (int i = 0; i < CELLS.length; i++) {
            int sx = x + POSITIONS[i][0];
            int sy = y + POSITIONS[i][1];
            PESideMode mode = handler.getMode(EnergySideConfiguration.sideFor(handler.getSideFacing(), CELLS[i]));
            boolean hover = mouseX >= sx && mouseX < sx + SIZE && mouseY >= sy && mouseY < sy + SIZE;
            context.fill(sx, sy, sx + SIZE, sy + SIZE, hover ? 0xFFFFFFFF : 0xFF30343A);
            context.fill(sx + 3, sy + 3, sx + SIZE - 3, sy + SIZE - 3, mode.color());
            Text label = Text.translatable("gui.phaseteleporters.side." + SIDE_KEYS[i]);
            context.drawCenteredTextWithShadow(textRenderer, label, sx + SIZE / 2, sy + 18, 0xFFFFFFFF);
        }
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 9, 10, 0xFF303030, false);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int x = (width - backgroundWidth) / 2;
            int y = (height - backgroundHeight) / 2;
            if (mouseX >= x + 222 && mouseX < x + 244 && mouseY >= y + 5 && mouseY < y + 25) {
                close();
                return true;
            }
            for (int i = 0; i < CELLS.length; i++) {
                int sx = x + POSITIONS[i][0];
                int sy = y + POSITIONS[i][1];
                if (mouseX >= sx && mouseX < sx + SIZE && mouseY >= sy && mouseY < sy + SIZE) {
                    if (client != null && client.interactionManager != null)
                        client.interactionManager.clickButton(handler.syncId, CELLS[i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        for (int i = 0; i < CELLS.length; i++) {
            int sx = x + POSITIONS[i][0];
            int sy = y + POSITIONS[i][1];
            if (mouseX >= sx && mouseX < sx + SIZE && mouseY >= sy && mouseY < sy + SIZE) {
                Text side = Text.translatable("gui.phaseteleporters.side." + SIDE_KEYS[i]);
                Text mode = Text.translatable(handler.getMode(EnergySideConfiguration.sideFor(
                        handler.getSideFacing(), CELLS[i])).translationKey());
                context.drawTooltip(textRenderer, Text.literal(side.getString() + ": " + mode.getString()),
                        mouseX, mouseY);
                break;
            }
        }
    }
}
