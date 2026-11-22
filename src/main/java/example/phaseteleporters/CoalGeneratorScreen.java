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
    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 8;

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
        if (!handler.getSlot(1).hasStack()) {
            context.drawTexture(ENERGY_OUTPUT_TEXTURE, x + 7, y + 61, 0, 0, 18, 18, 18, 18);
        }

        // Огонь.
        drawBurnProgress(context);
    }

    private void drawEnergy(DrawContext context) {
        EnergyBarRenderer.draw(context, x + ENERGY_X, y + ENERGY_Y, handler.getEnergy(), handler.getMaxEnergy());
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

        if (EnergyBarRenderer.contains(x + ENERGY_X, y + ENERGY_Y, mouseX, mouseY)) {
            context.drawTooltip(textRenderer, java.util.List.of(
                    Text.literal(handler.getGeneration() + " J/t").formatted(net.minecraft.util.Formatting.GREEN),
                    Text.literal(PEGuiText.energy(handler.getEnergy(), handler.getMaxEnergy()))),
                    mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)
                || super.mouseClicked(mouseX, mouseY, button);
    }
}
