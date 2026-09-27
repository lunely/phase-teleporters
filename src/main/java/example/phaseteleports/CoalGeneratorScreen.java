package example.phaseteleports;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CoalGeneratorScreen
        extends HandledScreen<CoalGeneratorScreenHandler> {

    private static final Identifier GUI_TEXTURE =
            Identifier.of(
                    "phaseteleports",
                    "textures/gui/coalgenerator.png"
            );

    private static final Identifier BURN_TEXTURE =
            Identifier.of(
                    "phaseteleports",
                    "textures/gui/coal_generator_burn.png"
            );

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

        addDrawableChild(
                EnergyConfigurationButton.create(
                        x - 24,
                        y + 6,
                        handler
                )
        );
    }

    @Override
    protected void drawBackground(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY
    ) {
        // Основной интерфейс генератора.
        context.drawTexture(
                GUI_TEXTURE,
                x,
                y,
                0,
                0,
                backgroundWidth,
                backgroundHeight
        );

        // Огонь.
        drawBurnProgress(context);

        // Полоска энергии.
        EnergyBarRenderer.draw(
                context,
                x - 20,
                y + 20,
                handler.getEnergy(),
                handler.getMaxEnergy()
        );
    }

    private void drawBurnProgress(DrawContext context) {
        int fuelTime = handler.getFuelTime();
        int burnTime = handler.getBurnTime();

        if (fuelTime <= 0 || burnTime <= 0) {
            return;
        }

        // Размер картинки огня.
        int flameWidth = 14;
        int flameHeight = 14;

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

        /*
         * Огонь уменьшается сверху вниз,
         * как индикатор топлива у печки.
         *
         * Эти координаты под твой новый GUI.
         */
        int flameX = x + 107;
        int flameY = y + 32;

        int offset = flameHeight - visibleHeight;

        context.drawTexture(
                BURN_TEXTURE,

                flameX,
                flameY + offset,

                0,
                offset,

                flameWidth,
                visibleHeight,

                flameWidth,
                flameHeight
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