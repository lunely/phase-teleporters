package example.phaseteleporters;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class InfusionStationScreen extends HandledScreen<InfusionStationScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();
    private static final Identifier GUI_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/infusion_station.png");
    private static final Identifier ENERGY_SLOT_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");
    private static final Identifier EMPTY_BAR_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_filled.png");
    private static final Identifier WATER_TEXTURE =
            Identifier.of("minecraft", "textures/block/water_still.png");
    private static final int BAR_Y = 8;
    private static final int BAR_WIDTH = 16;
    private static final int BAR_HEIGHT = 48;
    private static final int ENERGY_X = 8;
    private static final int INFUSION_X = 33;
    private static final int CLEAR_X = 59;
    private static final int CLEAR_Y = 64;
    private static final int CLEAR_WIDTH = 30;
    private static final int CLEAR_HEIGHT = 13;
    private static final int[][] CLEAR_LETTERS = {
            {0b111, 0b100, 0b100, 0b100, 0b111}, // C
            {0b100, 0b100, 0b100, 0b100, 0b111}, // L
            {0b111, 0b100, 0b110, 0b100, 0b111}, // E
            {0b111, 0b101, 0b111, 0b101, 0b101}, // A
            {0b110, 0b101, 0b110, 0b101, 0b101}  // R
    };

    public InfusionStationScreen(InfusionStationScreenHandler handler, PlayerInventory inventory, Text title) {
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(GUI_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight);
        drawEnergy(context);
        drawInfusion(context);
        context.drawTexture(ENERGY_SLOT_TEXTURE, x + 8, y + 62, 0, 0, 16, 16, 16, 16);
        drawClearButton(context, mouseX, mouseY);

        int progress = handler.getProcessTime() == 0 ? 0
                : Math.clamp(22 * handler.getProgress() / handler.getProcessTime(), 0, 22);
        if (progress > 0) {
            context.drawTexture(GUI_TEXTURE, x + 100, y + 31, 180, 4,
                    progress, 15, 256, 256);
        }
    }

    private void drawEnergy(DrawContext context) {
        int left = x + ENERGY_X;
        int top = y + BAR_Y;
        int bottom = top + BAR_HEIGHT;
        drawBarFrame(context, left, top, bottom);
        long capacity = handler.getMaxEnergy();
        if (capacity <= 0) return;
        int filled = (int) Math.min(BAR_HEIGHT,
                Math.max(0, handler.getEnergy()) * BAR_HEIGHT / capacity);
        if (filled > 0) {
            context.drawTexture(FILLED_ENERGY_TEXTURE, left, bottom - filled, 0,
                    BAR_HEIGHT - filled, BAR_WIDTH, filled, BAR_WIDTH, BAR_HEIGHT);
        }
    }

    private void drawInfusion(DrawContext context) {
        int left = x + INFUSION_X;
        int top = y + BAR_Y;
        int amount = handler.getInfusionAmount();
        InfusionResource resource = handler.getInfusionResource();
        if (amount > 0 && resource != InfusionResource.NONE) {
            int filled = Math.clamp((amount * BAR_HEIGHT + InfusionStationBlockEntity.MAX_INFUSION - 1)
                    / InfusionStationBlockEntity.MAX_INFUSION, 1, BAR_HEIGHT);
            int fillTop = top + BAR_HEIGHT - filled;
            int color = resource.barColor();
            int red = (((color >> 16) & 0xFF) * 3 + 0x8B * 2) / 5;
            int green = (((color >> 8) & 0xFF) * 3 + 0x8B * 2) / 5;
            int blue = ((color & 0xFF) * 3 + 0x8B * 2) / 5;
            context.fill(left, fillTop, left + BAR_WIDTH, top + BAR_HEIGHT,
                    0xFF000000 | red << 16 | green << 8 | blue);
            RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F,
                    ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 0.38F);
            try {
                int frame = client != null && client.world != null
                        ? (int) ((client.world.getTime() / 5) % 32) : 0;
                for (int segmentTop = top; segmentTop < top + BAR_HEIGHT; segmentTop += 16) {
                    int visibleTop = Math.max(fillTop, segmentTop);
                    int visibleBottom = Math.min(top + BAR_HEIGHT, segmentTop + 16);
                    if (visibleTop < visibleBottom) {
                        int visibleHeight = visibleBottom - visibleTop;
                        context.drawTexture(WATER_TEXTURE, left, visibleTop, 0,
                                frame * 16 + visibleTop - segmentTop,
                                BAR_WIDTH, visibleHeight, 16, 512);
                    }
                }
            } finally {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }

        for (int tick = 0; tick < 16; tick++) {
            int tickY = top + 1 + tick * 3;
            int tickWidth = (tick & 1) == 0 ? 6 : 8;
            context.fill(left, tickY, left + tickWidth, tickY + 1, 0xFFB31212);
        }
    }

    private void drawBarFrame(DrawContext context, int left, int top, int bottom) {
        context.fill(left - 1, top - 1, left + BAR_WIDTH + 1, bottom + 1, 0xFF373737);
        context.drawTexture(EMPTY_BAR_TEXTURE, left, top, 0, 0,
                BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);
    }

    private boolean canClear() {
        return handler.getProgress() == 0 && handler.getInfusionAmount() > 0;
    }

    private boolean isOverClearButton(double mouseX, double mouseY) {
        return mouseX >= x + CLEAR_X && mouseX < x + CLEAR_X + CLEAR_WIDTH
                && mouseY >= y + CLEAR_Y && mouseY < y + CLEAR_Y + CLEAR_HEIGHT;
    }

    private void drawClearButton(DrawContext context, int mouseX, int mouseY) {
        int left = x + CLEAR_X;
        int top = y + CLEAR_Y;
        boolean enabled = canClear();
        boolean hovered = enabled && isOverClearButton(mouseX, mouseY);
        context.fill(left, top, left + CLEAR_WIDTH, top + CLEAR_HEIGHT,
                hovered ? 0xFF80D770 : 0xFF30343A);
        context.fill(left + 1, top + 1, left + CLEAR_WIDTH - 1, top + CLEAR_HEIGHT - 1,
                hovered ? 0xFF536853 : enabled ? 0xFF414B43 : 0xFF555555);
        int letterX = left + (CLEAR_WIDTH - (CLEAR_LETTERS.length * 4 - 1)) / 2;
        int letterY = top + (CLEAR_HEIGHT - 5) / 2;
        int letterColor = enabled ? 0xFFE6E6E6 : 0xFF999999;
        for (int[] letter : CLEAR_LETTERS) {
            for (int row = 0; row < letter.length; row++) {
                for (int column = 0; column < 3; column++) {
                    if ((letter[row] & (1 << (2 - column))) != 0) {
                        context.fill(letterX + column, letterY + row,
                                letterX + column + 1, letterY + row + 1, letterColor);
                    }
                }
            }
            letterX += 4;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)) return true;
        if (button == 0 && isOverClearButton(mouseX, mouseY)) {
            if (canClear() && client != null && client.interactionManager != null) {
                client.interactionManager.clickButton(handler.syncId, 0);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (mouseX >= x + ENERGY_X && mouseX < x + ENERGY_X + BAR_WIDTH
                && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_HEIGHT) {
            context.drawTooltip(textRenderer,
                    Text.literal(PEGuiText.energy(handler.getEnergy(), handler.getMaxEnergy())),
                    mouseX, mouseY);
        } else if (mouseX >= x + INFUSION_X && mouseX < x + INFUSION_X + BAR_WIDTH
                && mouseY >= y + BAR_Y && mouseY < y + BAR_Y + BAR_HEIGHT) {
            InfusionResource resource = handler.getInfusionResource();
            Text resourceName = resource == InfusionResource.NONE
                    ? Text.translatable("gui.phaseteleporters.infusion.empty") : resource.item().getName();
            context.drawTooltip(textRenderer,
                    resourceName.copy().append(" "
                            + handler.getInfusionAmount() + " / " + InfusionStationBlockEntity.MAX_INFUSION),
                    mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }
}
