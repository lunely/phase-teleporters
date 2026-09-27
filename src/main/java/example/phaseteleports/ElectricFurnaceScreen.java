package example.phaseteleports;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ElectricFurnaceScreen extends HandledScreen<ElectricFurnaceScreenHandler> {

    private static final Identifier FURNACE_TEXTURE =
            Identifier.of("phaseteleports", "textures/gui/electric_furnace.png");

    public ElectricFurnaceScreen(
            ElectricFurnaceScreenHandler handler,
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
        // Фон GUI
        context.drawTexture(
                FURNACE_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight
        );

        // Прогресс плавки
        int processTime = handler.getProcessTime();

        if (processTime > 0) {
            int progress = 22 * handler.getProgress() / processTime;

            drawProgressArrow(
                    context,
                    x + 80,
                    y + 35,
                    progress
            );
        }

        // PE bar
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
            int width
    ) {
        if (width <= 0) {
            return;
        }

        int color = 0xFFFFFFFF;
        width = Math.min(width, 22);

        // Горизонтальная часть стрелки
        int shaftWidth = Math.min(width, 14);

        if (shaftWidth > 0) {
            context.fill(
                    x,
                    y + 6,
                    x + shaftWidth,
                    y + 9,
                    color
            );
        }

        // Наконечник стрелки — теперь сужается вправо
        if (width > 14) {
            int tipWidth = width - 14;

            for (int i = 0; i < tipWidth; i++) {
                int px = x + 14 + i;

                // Было наоборот, теперь широкий у основания и острый справа
                int halfHeight = Math.max(1, 7 - i);

                context.fill(
                        px,
                        y + 7 - halfHeight,
                        px + 1,
                        y + 8 + halfHeight,
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