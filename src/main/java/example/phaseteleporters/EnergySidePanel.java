package example.phaseteleporters;

import example.phaseteleporters.energy.PESideMode;
import example.phaseteleporters.energy.PERedstoneMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Side configuration drawer using the GUI artwork at its native pixel size. */
public final class EnergySidePanel {
    private static final Identifier BACKGROUND = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/config.png");
    private static final Identifier BUTTON_ICON = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/side_config.png");
    private static final Identifier BUTTON_TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/config_button.png");
    private static final Identifier REDSTONE_BACKGROUND = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/config_redstone.png");
    private static final Identifier REDSTONE_BUTTON_TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/redstone_button.png");
    private static final Identifier SECURITY_BACKGROUND = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/config_security.png");
    private static final Identifier SECURITY_BUTTON_TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/slot/security_button.png");
    private static final ItemStack REDSTONE_ICON = new ItemStack(Items.REDSTONE);
    private static final ItemStack GUNPOWDER_ICON = new ItemStack(Items.GUNPOWDER);
    private static final ItemStack REDSTONE_TORCH_ICON = new ItemStack(Items.REDSTONE_TORCH);
    private static final Identifier UNLIT_REDSTONE_TORCH = Identifier.ofVanilla("textures/block/redstone_torch_off.png");
    private static final Identifier VANILLA_BUTTON = Identifier.ofVanilla("widget/button");
    private static final Identifier VANILLA_BUTTON_HIGHLIGHTED = Identifier.ofVanilla("widget/button_highlighted");
    private static final Identifier VANILLA_BUTTON_DISABLED = Identifier.ofVanilla("widget/button_disabled");
    private static final int PANEL_U = 12;
    private static final int PANEL_V = 9;
    private static final int WIDTH = 101;
    private static final int HEIGHT = 91;
    private static final int BUTTON_U = 7;
    private static final int BUTTON_V = 5;
    private static final int BUTTON_WIDTH = 25;
    private static final int BUTTON_HEIGHT = 22;
    private static final int REDSTONE_OFFSET_Y = BUTTON_HEIGHT + 3;
    private static final int ICON_SIZE = 18;
    private static final int CELL = 18;
    private static final int REDSTONE_MODE_Y = 27;
    private static final int REDSTONE_MODE_SIZE = 20;
    private static final int[] REDSTONE_MODE_X = {12, 39, 66};
    private static final String[] REDSTONE_MODE_KEYS = {"ignored", "without_signal", "with_signal"};
    // Five faces form a plus; the back face sits in its lower-left corner.
    private static final int[][] CELLS = {
            {46, 27}, {26, 46}, {46, 46}, {66, 46}, {46, 65}, {26, 65}
    };
    private static final String[] LABELS = {"top", "left", "front", "right", "bottom", "back"};
    private static final String[] MODES = {
            "input", "output", "input_output", "disabled", "item_input", "item_output",
            "energy_item_input", "energy_item_output"
    };

    private boolean open;
    private boolean redstoneOpen;
    private boolean securityOpen;
    private final boolean stackedButtons;

    public EnergySidePanel() { this(false); }

    public EnergySidePanel(boolean stackedButtons) {
        this.stackedButtons = stackedButtons;
    }

    public void closeAll() {
        open = false;
        redstoneOpen = false;
        securityOpen = false;
    }

    private int panelX(int guiX) { return guiX - WIDTH + 3; }
    private int buttonX(int guiX) { return guiX - BUTTON_WIDTH + 3; }
    private int panelY(int guiY) { return guiY + 5; }
    private int buttonY(int guiY) { return guiY + 5; }
    private int redstoneY(int guiY) { return buttonY(guiY) + REDSTONE_OFFSET_Y; }
    private int redstoneButtonY(int guiY) {
        return open ? panelY(guiY) + HEIGHT : redstoneY(guiY);
    }
    private int securityX(int guiX, int guiWidth) {
        return stackedButtons ? panelX(guiX) : guiX + guiWidth - 3;
    }
    private int securityButtonX(int guiX, int guiWidth) {
        return stackedButtons ? buttonX(guiX) : guiX + guiWidth - 3;
    }
    private int securityButtonY(int guiY) {
        if (!stackedButtons) return buttonY(guiY);
        if (open) return panelY(guiY) + HEIGHT + BUTTON_HEIGHT + 3;
        if (redstoneOpen) return redstoneY(guiY) + HEIGHT;
        return buttonY(guiY) + 2 * (BUTTON_HEIGHT + 3);
    }

    public void drawBehind(DrawContext context, int guiX, int guiY, int guiWidth,
            int mouseX, int mouseY, EnergySideScreenHandler handler) {
        if (securityOpen) drawSecurityPanel(context, guiX, guiY, guiWidth, mouseX, mouseY, handler);
        if (securityOpen && stackedButtons) return;
        if (redstoneOpen) drawRedstonePanel(context, guiX, guiY, mouseX, mouseY, handler);
        if (!open && !redstoneOpen) {
            drawButton(context, guiX, guiY);
            drawRedstoneButton(context, guiX, guiY);
            if (!securityOpen) drawSecurityButton(context, guiX, guiY, guiWidth);
            return;
        }
        if (redstoneOpen) {
            drawButton(context, guiX, guiY);
            drawSecurityButton(context, guiX, guiY, guiWidth);
            return;
        }
        int px = panelX(guiX);
        int py = panelY(guiY);
        context.drawTexture(BACKGROUND, px, py, PANEL_U, PANEL_V, WIDTH, HEIGHT, 256, 256);
        drawHeader(context, px, py);
        drawSideArea(context, px, py);
        for (int i = 0; i < CELLS.length; i++) {
            int sx = px + CELLS[i][0];
            int sy = py + CELLS[i][1];
            PESideMode mode = handler.getSideMode(EnergySideConfiguration.sideFor(handler.getSideFacing(), i));
            drawModeSlot(context, sx, sy);
            context.fill(sx + 2, sy + 4, sx + 14, sy + 16, mode.color());
            context.fill(sx + 3, sy + 5, sx + 13, sy + 6, 0x50FFFFFF);
            context.fill(sx + 3, sy + 14, sx + 13, sy + 15, 0x40000000);
            String sign = switch (mode) {
                case INPUT, ITEM_INPUT, ENERGY_ITEM_INPUT -> "+";
                case OUTPUT, ITEM_OUTPUT, ENERGY_ITEM_OUTPUT -> "−";
                case INPUT_OUTPUT -> "±";
                case DISABLED -> "";
            };
            if (!sign.isEmpty()) {
                TextRenderer font = MinecraftClient.getInstance().textRenderer;
                context.drawText(font, sign, sx + 8 - font.getWidth(sign) / 2,
                        sy + 5, 0xFFFFFFFF, true);
            }
            if (inside(mouseX, mouseY, sx, sy, CELL, CELL)) {
                context.fill(sx + 2, sy + 4, sx + 14, sy + 16, 0x30FFFFFF);
            }
        }
        drawRedstoneButton(context, guiX, guiY);
        drawSecurityButton(context, guiX, guiY, guiWidth);
    }

    private void drawSecurityPanel(DrawContext context, int guiX, int guiY, int guiWidth,
            int mouseX, int mouseY, EnergySideScreenHandler handler) {
        int px = securityX(guiX, guiWidth);
        int py = guiY + 5;
        context.drawTexture(SECURITY_BACKGROUND, px, py, PANEL_U, PANEL_V, WIDTH, HEIGHT, 256, 256);
        context.fill(px + 8, py + 23, px + 96, py + 86, 0xFF0B5026);
        context.fill(px + 9, py + 24, px + 95, py + 85, 0xFF167536);
        drawLockIcon(context, px + 7, py + 3, !handler.isPublicAccess());
        drawFittedText(context, MinecraftClient.getInstance().textRenderer,
                Text.translatable("gui.phaseteleporters.security.title"),
                px + 25, py + 7, WIDTH - 28, 0xFFFFFFFF);
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        for (int i = 0; i < 2; i++) {
            int bx = px + 19 + i * 44;
            int by = py + 30;
            boolean selected = handler.isPublicAccess() == (i == 0);
            boolean owner = handler.isSecurityOwner();
            boolean hovered = owner && inside(mouseX, mouseY, bx, by, 20, 20);
            context.drawGuiTexture(selected || !owner ? VANILLA_BUTTON_DISABLED
                            : hovered ? VANILLA_BUTTON_HIGHLIGHTED : VANILLA_BUTTON,
                    bx, by, 20, 20);
            drawLockIcon(context, bx + 2, by + 2, i == 1);
            if (selected || !owner) context.fill(bx + 2, by + 2, bx + 18, by + 18, 0x77555555);
            Text label = Text.translatable(i == 0
                    ? "gui.phaseteleporters.security.public" : "gui.phaseteleporters.security.private");
            drawFittedText(context, font, label, bx - 5, py + 55, 30, 0xFFFFFFFF);
        }
        Text status = Text.translatable(handler.isSecurityOwner()
                ? "gui.phaseteleporters.security.owner" : "gui.phaseteleporters.security.owner_only");
        drawFittedText(context, font, status, px + 11, py + 74, WIDTH - 18, 0xFFFFFFFF);
    }

    private static void drawLockIcon(DrawContext context, int x, int y, boolean closed) {
        int outline = 0xFF283236;
        int white = 0xFFF8FBFC;
        int silver = 0xFFCBD5D9;
        int shadow = 0xFF87959B;
        if (closed) {
            context.fill(x + 4, y + 1, x + 12, y + 3, outline);
            context.fill(x + 3, y + 3, x + 5, y + 9, outline);
            context.fill(x + 11, y + 3, x + 13, y + 9, outline);
            context.fill(x + 5, y + 2, x + 11, y + 3, white);
            context.fill(x + 4, y + 4, x + 5, y + 8, white);
            context.fill(x + 11, y + 4, x + 12, y + 8, silver);
        } else {
            context.fill(x + 3, y + 1, x + 11, y + 3, outline);
            context.fill(x + 2, y + 3, x + 4, y + 9, outline);
            context.fill(x + 9, y + 3, x + 11, y + 6, outline);
            context.fill(x + 4, y + 2, x + 10, y + 3, white);
            context.fill(x + 3, y + 4, x + 4, y + 8, white);
            context.fill(x + 10, y + 4, x + 11, y + 5, silver);
        }
        context.fill(x + 2, y + 8, x + 14, y + 15, outline);
        context.fill(x + 3, y + 9, x + 13, y + 14, white);
        context.fill(x + 11, y + 9, x + 13, y + 14, silver);
        context.fill(x + 3, y + 13, x + 13, y + 14, shadow);
        context.fill(x + 4, y + 9, x + 10, y + 10, 0xFFFFFFFF);
        context.fill(x + 7, y + 10, x + 9, y + 12, outline);
        context.fill(x + 8, y + 12, x + 9, y + 13, outline);
    }

    private static void drawRedstonePanel(DrawContext context, int guiX, int guiY,
            int mouseX, int mouseY, EnergySideScreenHandler handler) {
        int px = guiX - WIDTH + 3;
        int py = guiY + 5 + REDSTONE_OFFSET_Y;
        context.drawTexture(REDSTONE_BACKGROUND, px, py, PANEL_U, PANEL_V,
                WIDTH, HEIGHT, 256, 256);
        context.fill(px + 8, py + 23, px + 96, py + 86, 0xFF5B0B06);
        context.fill(px + 9, py + 24, px + 95, py + 85, 0xFF8C180D);
        context.drawItem(REDSTONE_ICON, px + 6, py + 3);
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        drawFittedText(context, font,
                Text.translatable("gui.phaseteleporters.redstone_control_title"),
                px + 25, py + 7, WIDTH - 28, 0xFFFFFFFF);
        PERedstoneMode mode = handler.getRedstoneMode();
        for (int i = 0; i < REDSTONE_MODE_X.length; i++) {
            int bx = px + REDSTONE_MODE_X[i];
            int by = py + REDSTONE_MODE_Y;
            boolean selected = mode.ordinal() == i;
            boolean hovered = inside(mouseX, mouseY, bx, by, REDSTONE_MODE_SIZE, REDSTONE_MODE_SIZE);
            context.drawGuiTexture(selected ? VANILLA_BUTTON_DISABLED
                            : hovered ? VANILLA_BUTTON_HIGHLIGHTED : VANILLA_BUTTON,
                    bx, by, REDSTONE_MODE_SIZE, REDSTONE_MODE_SIZE);
            if (i == 0) context.drawItem(GUNPOWDER_ICON, bx + 2, by + 2);
            else if (i == 1) context.drawTexture(UNLIT_REDSTONE_TORCH,
                    bx + 2, by + 2, 0, 0, 16, 16, 16, 16);
            else context.drawItem(REDSTONE_TORCH_ICON, bx + 2, by + 2);
            if (selected) context.fill(bx + 2, by + 2, bx + 18, by + 18, 0xAA555555);
        }
        Text state = Text.translatable("gui.phaseteleporters.redstone.state",
                Text.translatable("gui.phaseteleporters.redstone.mode." + REDSTONE_MODE_KEYS[mode.ordinal()]));
        boolean enabled = mode.allowsWork(handler.hasRedstoneSignal());
        Text status = Text.translatable("gui.phaseteleporters.redstone.status");
        drawFittedText(context, font, state, px + 10, py + 54, WIDTH - 18, 0xFFFFFFFF);
        context.drawText(font, status, px + 10, py + 69, 0xFFFFFFFF, true);
        context.drawText(font, Text.translatable("gui.phaseteleporters.redstone." + (enabled ? "enabled" : "disabled")),
                px + 10 + font.getWidth(status), py + 69,
                enabled ? 0xFF55FF55 : 0xFFFF5555, true);
    }

    private static void drawHeader(DrawContext context, int px, int py) {
        context.drawTexture(BUTTON_ICON, px + 6, py + 2, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        context.drawText(MinecraftClient.getInstance().textRenderer,
                Text.translatable("gui.phaseteleporters.side_config_title"),
                px + 25, py + 7, 0xFF404040, false);
    }

    private static void drawSideArea(DrawContext context, int px, int py) {
        context.fill(px + 8, py + 23, px + 96, py + 86, 0xFF777777);
        context.fill(px + 9, py + 24, px + 95, py + 85, 0xFFA9A9A9);
        context.fill(px + 9, py + 24, px + 95, py + 25, 0xFF989898);
        context.fill(px + 9, py + 24, px + 10, py + 85, 0xFF989898);
    }

    private void drawButton(DrawContext context, int guiX, int guiY) {
        int bx = buttonX(guiX);
        int by = buttonY(guiY);
        context.drawTexture(BUTTON_TEXTURE, bx, by, BUTTON_U, BUTTON_V,
                BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
        context.drawTexture(BUTTON_ICON, bx + (guiX - bx - ICON_SIZE) / 2 + 1,
                by + (BUTTON_HEIGHT - ICON_SIZE) / 2, 0, 0,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    private void drawRedstoneButton(DrawContext context, int guiX, int guiY) {
        int bx = buttonX(guiX);
        int by = redstoneButtonY(guiY);
        context.drawTexture(REDSTONE_BUTTON_TEXTURE, bx, by, BUTTON_U, BUTTON_V,
                BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
        context.drawItem(REDSTONE_ICON, bx + (guiX - bx - 16) / 2 + 1,
                by + (BUTTON_HEIGHT - 16) / 2);
    }

    private void drawSecurityButton(DrawContext context, int guiX, int guiY, int guiWidth) {
        int bx = securityButtonX(guiX, guiWidth);
        int by = securityButtonY(guiY);
        context.drawTexture(SECURITY_BUTTON_TEXTURE, bx, by, BUTTON_U, BUTTON_V,
                BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
        drawLockIcon(context, bx + 4, by + 1, true);
    }

    public boolean click(double mouseX, double mouseY, int button, int guiX, int guiY,
            int guiWidth, ScreenHandler handler) {
        if (button != 0) return false;
        int px = panelX(guiX);
        int py = panelY(guiY);
        if ((!securityOpen || !stackedButtons) && !open
                && inside(mouseX, mouseY, buttonX(guiX), buttonY(guiY),
                guiX - buttonX(guiX), BUTTON_HEIGHT)) {
            open = true;
            redstoneOpen = false;
            securityOpen = false;
            playClick();
            return true;
        }
        if ((!securityOpen || !stackedButtons) && !redstoneOpen
                && inside(mouseX, mouseY, buttonX(guiX), redstoneButtonY(guiY),
                guiX - buttonX(guiX), BUTTON_HEIGHT)) {
            redstoneOpen = true;
            open = false;
            securityOpen = false;
            playClick();
            return true;
        }
        int sx = securityOpen ? securityX(guiX, guiWidth) : securityButtonX(guiX, guiWidth);
        if (!securityOpen && inside(mouseX, mouseY, sx, securityButtonY(guiY), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            securityOpen = true;
            open = false;
            redstoneOpen = false;
            playClick();
            return true;
        }
        if (securityOpen) {
            int sy = panelY(guiY);
            if (inside(mouseX, mouseY, sx + 5, sy + 2, WIDTH - 10, 19)) {
                securityOpen = false;
                playClick();
                return true;
            }
            for (int i = 0; i < 2; i++) {
                if (inside(mouseX, mouseY, sx + 19 + i * 44, sy + 30, 20, 20)) {
                    if (handler instanceof EnergySideScreenHandler sideHandler
                            && sideHandler.isSecurityOwner()
                            && sideHandler.isPublicAccess() != (i == 0)) {
                        var interaction = MinecraftClient.getInstance().interactionManager;
                        if (interaction != null) interaction.clickButton(handler.syncId,
                                EnergySideConfiguration.SECURITY_FIRST_BUTTON + i);
                        playClick();
                    }
                    return true;
                }
            }
            return inside(mouseX, mouseY, sx, sy, WIDTH, HEIGHT);
        }
        if (redstoneOpen) {
            int ry = redstoneY(guiY);
            if (inside(mouseX, mouseY, px + 5, ry + 2, guiX - px - 5, 19)) {
                redstoneOpen = false;
                playClick();
                return true;
            }
            for (int i = 0; i < REDSTONE_MODE_X.length; i++) {
                if (inside(mouseX, mouseY, px + REDSTONE_MODE_X[i], ry + REDSTONE_MODE_Y,
                        REDSTONE_MODE_SIZE, REDSTONE_MODE_SIZE)) {
                    if (handler instanceof EnergySideScreenHandler sideHandler
                            && sideHandler.getRedstoneMode().ordinal() == i) return true;
                    var interaction = MinecraftClient.getInstance().interactionManager;
                    if (interaction != null) interaction.clickButton(handler.syncId,
                            EnergySideConfiguration.REDSTONE_FIRST_BUTTON + i);
                    playClick();
                    return true;
                }
            }
            return inside(mouseX, mouseY, px, ry, guiX - px, HEIGHT);
        }
        if (!open) return false;
        if (inside(mouseX, mouseY, px + 5, py + 2, guiX - px - 5, 19)) {
            open = false;
            playClick();
            return true;
        }
        for (int i = 0; i < CELLS.length; i++) {
            if (inside(mouseX, mouseY, px + CELLS[i][0], py + CELLS[i][1], CELL, CELL)) {
                var interaction = MinecraftClient.getInstance().interactionManager;
                if (interaction != null)
                    interaction.clickButton(handler.syncId,
                            EnergySideConfiguration.FIRST_BUTTON + i);
                playClick();
                return true;
            }
        }
        return inside(mouseX, mouseY, px, py, guiX - px, HEIGHT);
    }

    public void tooltip(DrawContext context, TextRenderer font, int guiX, int guiY, int guiWidth,
            int mouseX, int mouseY, EnergySideScreenHandler handler) {
        int px = panelX(guiX);
        int py = panelY(guiY);
        if ((!securityOpen || !stackedButtons) && !open
                && inside(mouseX, mouseY, buttonX(guiX), buttonY(guiY),
                guiX - buttonX(guiX), BUTTON_HEIGHT)) {
            context.drawTooltip(font, Text.translatable("gui.phaseteleporters.configure_sides"), mouseX, mouseY);
            return;
        }
        if ((!securityOpen || !stackedButtons) && !redstoneOpen
                && inside(mouseX, mouseY, buttonX(guiX), redstoneButtonY(guiY),
                guiX - buttonX(guiX), BUTTON_HEIGHT)) {
            context.drawTooltip(font, Text.translatable("gui.phaseteleporters.redstone_control_title"), mouseX, mouseY);
            return;
        }
        int sx = securityOpen ? securityX(guiX, guiWidth) : securityButtonX(guiX, guiWidth);
        if (!securityOpen && inside(mouseX, mouseY, sx, securityButtonY(guiY), BUTTON_WIDTH, BUTTON_HEIGHT)) {
            context.drawTooltip(font, Text.translatable("gui.phaseteleporters.security.title"), mouseX, mouseY);
            return;
        }
        if (securityOpen) {
            for (int i = 0; i < 2; i++) {
                if (inside(mouseX, mouseY, sx + 19 + i * 44, panelY(guiY) + 30, 20, 20)) {
                    context.drawTooltip(font, Text.translatable(handler.isSecurityOwner()
                            ? (i == 0 ? "gui.phaseteleporters.security.public"
                                    : "gui.phaseteleporters.security.private")
                            : "gui.phaseteleporters.security.owner_only"), mouseX, mouseY);
                    return;
                }
            }
        }
        if (redstoneOpen) {
            int ry = redstoneY(guiY);
            for (int i = 0; i < REDSTONE_MODE_X.length; i++) {
                if (inside(mouseX, mouseY, px + REDSTONE_MODE_X[i], ry + REDSTONE_MODE_Y,
                        REDSTONE_MODE_SIZE, REDSTONE_MODE_SIZE)) {
                    context.drawTooltip(font,
                            Text.translatable("gui.phaseteleporters.redstone.mode." + REDSTONE_MODE_KEYS[i]),
                            mouseX, mouseY);
                    return;
                }
            }
        }
        if (!open) return;
        for (int i = 0; i < CELLS.length; i++) {
            if (inside(mouseX, mouseY, px + CELLS[i][0], py + CELLS[i][1], CELL, CELL)) {
                Text side = Text.translatable("gui.phaseteleporters.side." + LABELS[i]);
                PESideMode sideMode = handler.getSideMode(EnergySideConfiguration.sideFor(handler.getSideFacing(), i));
                Text mode = Text.translatable("gui.phaseteleporters.energy_mode."
                        + MODES[sideMode.ordinal()]).styled(style -> style.withColor(sideMode.color() & 0xFFFFFF));
                context.drawTooltip(font, Text.literal(side.getString() + ": ").append(mode), mouseX, mouseY);
                return;
            }
        }
    }

    private static void drawModeSlot(DrawContext context, int x, int y) {
        context.fill(x, y + 2, x + 16, y + 18, 0xFF4B4B4B);
        context.fill(x + 1, y + 3, x + 15, y + 17, 0xFFDCDCDC);
        context.fill(x + 2, y + 4, x + 14, y + 16, 0xFF8B8B8B);
    }

    private static void drawFittedText(DrawContext context, TextRenderer font, Text value,
            int x, int y, int maxWidth, int color) {
        float scale = Math.min(1.0F, (float) maxWidth / Math.max(1, font.getWidth(value)));
        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0F);
        context.drawText(font, value, 0, 0, color, true);
        context.getMatrices().pop();
    }

    private static void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
