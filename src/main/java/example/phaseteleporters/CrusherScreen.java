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
    private static final Identifier EMPTY_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_filled.png");
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 8;
    private static final int ENERGY_WIDTH = 16;
    private static final int ENERGY_HEIGHT = 48;

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
        context.drawTexture(ENERGY_SLOT_TEXTURE, x + 8, y + 62, 0, 0, 16, 16, 16, 16);

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
        int left = x + ENERGY_X;
        int top = y + ENERGY_Y;
        int bottom = top + ENERGY_HEIGHT;
        context.fill(left - 1, top - 1, left + ENERGY_WIDTH + 1, bottom + 1, 0xFF373737);
        context.drawTexture(EMPTY_ENERGY_TEXTURE, left, top, 0, 0,
                ENERGY_WIDTH, ENERGY_HEIGHT, ENERGY_WIDTH, ENERGY_HEIGHT);

        long capacity = handler.getMaxEnergy();
        if (capacity <= 0) return;
        int filled = (int) Math.min(ENERGY_HEIGHT,
                Math.max(0, handler.getEnergy()) * ENERGY_HEIGHT / capacity);
        if (filled <= 0) return;

        context.drawTexture(FILLED_ENERGY_TEXTURE, left, bottom - filled, 0,
                ENERGY_HEIGHT - filled, ENERGY_WIDTH, filled,
                ENERGY_WIDTH, ENERGY_HEIGHT);
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

        if (mouseX >= x + ENERGY_X && mouseX < x + ENERGY_X + ENERGY_WIDTH
                && mouseY >= y + ENERGY_Y && mouseY < y + ENERGY_Y + ENERGY_HEIGHT) {
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
