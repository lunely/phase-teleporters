package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CoalGeneratorScreen
        extends HandledScreen<CoalGeneratorScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();

    private static final Identifier GUI_TEXTURE =
            Identifier.of(
                    "phaseteleporters",
                    "textures/gui/coalgenerator.png"
            );

    private static final Identifier ENERGY_OUTPUT_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_output.png");
    private static final Identifier EMPTY_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_filled.png");
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 8;
    private static final int ENERGY_WIDTH = 16;
    private static final int ENERGY_HEIGHT = 48;

    public CoalGeneratorScreen(
            CoalGeneratorScreenHandler handler,
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
        // Основной интерфейс генератора.
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(
                GUI_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight
        );

        drawEnergy(context);
        context.drawTexture(ENERGY_OUTPUT_TEXTURE, x + 7, y + 61, 0, 0, 18, 18, 18, 18);

        // Огонь.
        drawBurnProgress(context);
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

    private void drawBurnProgress(DrawContext context) {
        int fuelTime = handler.getFuelTime();
        int burnTime = handler.getBurnTime();

        if (fuelTime <= 0 || burnTime <= 0) {
            return;
        }

        // The flame sprite is stored to the right of the GUI in coalgenerator.png.
        int flameWidth = 20;
        int flameHeight = 13;

        // Высота оставшегося огня.
        int visibleHeight = Math.clamp(
                (int) Math.ceil(
                        (double) burnTime
                                * flameHeight
                                / fuelTime
                ),
                0,
                flameHeight
        );

        if (visibleHeight <= 0) {
            return;
        }

        int flameX = x + 90;
        int flameY = y + 31;

        int offset = flameHeight - visibleHeight;

        context.drawTexture(
                GUI_TEXTURE,

                flameX,
                flameY + offset,

                180,
                3 + offset,

                flameWidth,
                visibleHeight,

                256,
                256
        );
    }

    @Override
    public void render(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        super.render(
                context,
                mouseX,
                mouseY,
                delta
        );

        drawMouseoverTooltip(
                context,
                mouseX,
                mouseY
        );

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
