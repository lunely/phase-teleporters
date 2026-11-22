package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CrusherScreen extends HandledScreen<CrusherScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();

    private static final Identifier CRUSHER_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/crusher.png");
    private static final Identifier ENERGY_SLOT_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 8;

    public CrusherScreen(
            CrusherScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title);

        backgroundWidth = 176;
        backgroundHeight = 166;
        playerInventoryTitleY = 72;
    }

    @Override
    protected void init() {
        super.init();
        titleX = backgroundWidth - 8 - textRenderer.getWidth(title);
        playerInventoryTitleX = backgroundWidth - 8 - textRenderer.getWidth(Text.translatable("container.inventory"));

    }

    @Override
    protected void drawBackground(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY
    ) {
        // Фон дробителя
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(
                CRUSHER_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight
        );

        drawEnergy(context);
        if (!handler.getSlot(2).hasStack()) {
            context.drawTexture(ENERGY_SLOT_TEXTURE, x + 8, y + 62, 0, 0, 16, 16, 16, 16);
        }

        // Прогресс дробления
        int processTime = handler.getProcessTime();

        if (processTime > 0) {
            int progress = 18 * handler.getProgress() / processTime;

            drawProgressArrow(
                    context,
                    x + 89,
                    y + 34,
                    progress
            );
        }
    }

    private void drawEnergy(DrawContext context) {
        EnergyBarRenderer.draw(context, x + ENERGY_X, y + ENERGY_Y, handler.getEnergy(), handler.getMaxEnergy());
    }

    private static void drawProgressArrow(
            DrawContext context,
            int x,
            int y,
            int height
    ) {
        if (height <= 0) {
            return;
        }

        // Белый цвет, как у стрелки электропечи
        int color = 0xFFFFFFFF;

        height = Math.min(height, 18);

        for (int row = 0; row < height; row++) {

            // Вертикальная ножка стрелки
            if (row <= 9) {
                context.fill(
                        x + 6,
                        y + row,
                        x + 9,
                        y + row + 1,
                        color
                );
            }

            // Наконечник стрелки
            else {
                int headRow = row - 10;

                int left = headRow;
                int right = 15 - headRow;

                context.fill(
                        x + left,
                        y + row,
                        x + right,
                        y + row + 1,
                        color
                );
            }
        }
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
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
