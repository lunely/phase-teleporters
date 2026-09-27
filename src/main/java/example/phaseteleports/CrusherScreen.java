package example.phaseteleports;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CrusherScreen extends HandledScreen<CrusherScreenHandler> {

    private static final Identifier CRUSHER_TEXTURE =
            Identifier.of("phaseteleports", "textures/gui/crusher.png");

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

        addDrawableChild(EnergyConfigurationButton.create(
                x - 24,
                y + 6,
                handler
        ));
    }

    @Override
    protected void drawBackground(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY
    ) {
        // Фон дробителя
        context.drawTexture(
                CRUSHER_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight
        );

        // Прогресс дробления
        int processTime = handler.getProcessTime();

        if (processTime > 0) {
            int progress = 17 * handler.getProgress() / processTime;

            drawProgressArrow(
                    context,
                    x + 80,
                    y + 35,
                    progress
            );
        }

        // PE bar слева
        EnergyBarRenderer.draw(
                context,
                x - 20,
                y + 20,
                handler.getEnergy(),
                handler.getMaxEnergy()
        );
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

        height = Math.min(height, 17);

        for (int row = 0; row < height; row++) {

            // Вертикальная ножка стрелки
            if (row <= 8) {
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
                int headRow = row - 9;

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

        EnergyBarRenderer.drawTooltip(
                context,
                textRenderer,
                x - 20,
                y + 20,
                mouseX,
                mouseY,
                handler.getEnergy(),
                handler.getMaxEnergy()
        );
    }
}