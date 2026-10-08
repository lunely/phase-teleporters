package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class EmergencyTeleportScreen<T extends EmergencyTeleportScreenHandler> extends HandledScreen<T> {
    private static final Identifier TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/emergency_teleporter.png");
    private static final Identifier REMOTE_TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/teleporter_emergency_menu.png");
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final int HEART_SIZE = 9;
    private static final int HEART_STEP = 10;
    private static final int HEART_ROW_WIDTH = 9 * HEART_STEP + HEART_SIZE;
    private static final Identifier LIST_BACKGROUND = texture("frequency_list_background.png");
    private static final float SETTINGS_TEXT_SCALE = 0.8f;
    private java.util.List<SettingsToggle> settings;
    private double settingsScroll;
    private boolean draggingSettingsScroll;
    private ButtonWidget less, more;
    private ButtonWidget bind;
    private final EnergySidePanel sidePanel = new EnergySidePanel();

    public EmergencyTeleportScreen(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = handler.remote() ? 176 : 175;
        backgroundHeight = handler.remote() ? 147 : 237;
    }

    @Override protected void init() {
        super.init();
        less = arrow(heartsX() - 11, false, EmergencyTeleportScreenHandler.LESS);
        more = arrow(heartsX() + HEART_ROW_WIDTH + 2, true, EmergencyTeleportScreenHandler.MORE);
        settings = java.util.List.of(
                toggle(EmergencyTeleportScreenHandler.ENABLED, "enabled"),
                toggle(EmergencyTeleportScreenHandler.FALLS, "rescue_falls"),
                toggle(EmergencyTeleportScreenHandler.TOTEM, "skip_totem"),
                toggle(EmergencyTeleportScreenHandler.WATER, "skip_water")
        );
        positionSettings();
        bind = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> click(EmergencyTeleportScreenHandler.BIND))
                .dimensions(x + (handler.remote() ? 49 : 41), y + (handler.remote() ? 123 : 131),
                        handler.remote() ? 78 : 92, handler.remote() ? 20 : 18).build());
    }

    private ButtonWidget arrow(int column, boolean forward, int id) {
        String sprite = "recipe_book/page_" + (forward ? "forward" : "backward");
        return addDrawableChild(new TexturedButtonWidget(x + column, y + heartsY() - 2, 9, 13,
                new ButtonTextures(Identifier.ofVanilla(sprite), Identifier.ofVanilla(sprite + "_highlighted")),
                button -> click(id), Text.literal(forward ? "+" : "−")) {
            @Override public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
                if (!active) context.setShaderColor(0.45f, 0.45f, 0.45f, 1);
                // Mouse clicks retain keyboard focus; only actual hover should highlight the arrow.
                context.drawGuiTexture(Identifier.ofVanilla(sprite + (active && isHovered() ? "_highlighted" : "")),
                        getX(), getY(), getWidth(), getHeight());
                if (!active) context.setShaderColor(1, 1, 1, 1);
            }
        });
    }
    private SettingsToggle toggle(int id, String key) {
        return addDrawableChild(new SettingsToggle(id, Text.translatable("gui.phaseteleporters.emergency." + key)));
    }
    private final class SettingsToggle extends ButtonWidget {
        private boolean checked;
        private SettingsToggle(int id, Text label) {
            super(0, 0, listWidth() - 10, Math.max(24,
                    (int) Math.ceil(textRenderer.wrapLines(label, settingsTextWidth()).size() * 9 * SETTINGS_TEXT_SCALE) + 8),
                    label, button -> click(id), DEFAULT_NARRATION_SUPPLIER);
        }
        @Override public void onPress() { checked = !checked; super.onPress(); }
        @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return insideSettings(mouseX, mouseY) && mouseX < x + listX() + listWidth() - 8
                    && super.mouseClicked(mouseX, mouseY, button);
        }
        @Override public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            context.enableScissor(x + listX() + 2, y + listY() + 2,
                    x + listX() + listWidth() - 8, y + listY() + listHeight() - 2);
            boolean hovered = isHovered() && insideSettings(mouseX, mouseY);
            if (hovered || isFocused()) context.fill(getX(), getY(), getX() + width, getY() + height, 0xFF285E60);
            var lines = textRenderer.wrapLines(getMessage(), settingsTextWidth());
            int textY = getY() + (height - (int) Math.ceil(lines.size() * 9 * SETTINGS_TEXT_SCALE)) / 2;
            context.getMatrices().push();
            context.getMatrices().translate(getX() + 3, textY, 0);
            context.getMatrices().scale(SETTINGS_TEXT_SCALE, SETTINGS_TEXT_SCALE, 1);
            for (int i = 0; i < lines.size(); i++)
                context.drawText(textRenderer, lines.get(i), 0, i * 9, 0xFF39FF39, false);
            context.getMatrices().pop();
            int sx = getX() + width - 21;
            int sy = getY() + (height - 10) / 2;
            context.fill(sx, sy, sx + 20, sy + 10, 0xFF171717);
            context.fill(sx + 1, sy + 1, sx + 19, sy + 9, checked ? 0xFF328343 : 0xFF555555);
            int thumbX = sx + (checked ? 11 : 1);
            context.fill(thumbX, sy + 1, thumbX + 8, sy + 9, hovered ? 0xFFFFFFFF : 0xFFD0D0D0);
            context.fill(thumbX + 1, sy + 2, thumbX + 7, sy + 3, 0xFFFFFFFF);
            context.disableScissor();
        }
    }
    private void click(int id) { if (client != null && client.interactionManager != null)
        client.interactionManager.clickButton(handler.syncId, id); }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        settings.get(0).checked = handler.enabled();
        settings.get(1).checked = handler.rescueFalls();
        settings.get(2).checked = handler.skipTotem();
        settings.get(3).checked = handler.skipWaterBucket();
        positionSettings();
        less.active = handler.threshold() > 1;
        more.active = handler.threshold() < 20;
        bind.setMessage(Text.translatable(handler.remote() ? "gui.phaseteleporters.emergency.unlink_remote"
                : handler.bound() ? "gui.phaseteleporters.emergency.unbind" : "gui.phaseteleporters.emergency.bind"));
        if (handler.remote()) {
            bind.active = handler.bound();
        }
        super.render(context, mouseX, mouseY, delta);
        if (!handler.remote()) sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
        if (!handler.remote() && mouseX >= x + 150 && mouseX < x + 169 && mouseY >= y + 97 && mouseY < y + 147)
            context.drawTooltip(textRenderer, Text.literal(PEGuiText.teleporterEnergy(handler.energy(), handler.capacity())), mouseX, mouseY);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        Identifier atlas = handler.remote() ? REMOTE_TEXTURE : TEXTURE;
        if (!handler.remote()) sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(atlas, x, y, handler.remote() ? 6 : 18, handler.remote() ? 3 : 4,
                backgroundWidth, backgroundHeight, 256, 256);
        drawSettingsList(context);
        if (!handler.remote()) {
            Identifier empty = texture("electric_furnace_energy_empty.png");
            Identifier filled = texture("electric_furnace_energy_filled.png");
            context.fill(x + 150, y + 97, x + 168, y + 147, 0xFF373737);
            context.drawTexture(empty, x + 151, y + 98, 0, 0, 16, 48, 16, 48);
            int amount = handler.capacity() <= 0 ? 0 : (int) Math.clamp(handler.energy() * 48 / handler.capacity(), 0, 48);
            if (amount > 0) context.drawTexture(filled, x + 151, y + 146 - amount, 0, 48 - amount, 16, amount, 16, 48);
            if (handler.getSlot(0).getStack().isEmpty()) {
                Identifier icon = texture("slot/chunkloader_slot.png");
                client.getTextureManager().getTexture(icon).setFilter(false, false);
                context.drawTexture(icon, x + 7, y + 130, 16, 16, 0, 0, 16, 16, 16, 16);
            }
            if (handler.getSlot(1).getStack().isEmpty())
                context.drawTexture(texture("slot/energy_slot.png"), x + 151, y + 77, 0, 0, 16, 16, 16, 16);
        }
        int hp = Math.clamp(handler.threshold(), 1, 20);
        for (int i = 0; i < 10; i++) {
            int hx = x + heartsX() + i * HEART_STEP;
            // Vanilla container and fills share the same native 9x9 origin and size.
            context.drawGuiTexture(HEART_CONTAINER, hx, y + heartsY(), HEART_SIZE, HEART_SIZE);
            if (hp >= i * 2 + 2)
                context.drawGuiTexture(HEART_FULL, hx, y + heartsY(), HEART_SIZE, HEART_SIZE);
            else if (hp == i * 2 + 1)
                context.drawGuiTexture(HEART_HALF, hx, y + heartsY(), HEART_SIZE, HEART_SIZE);
        }
    }

    @Override protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, (backgroundWidth - textRenderer.getWidth(title)) / 2, 6, 0x404040, false);
        Text threshold = Text.translatable("gui.phaseteleporters.emergency.threshold", handler.threshold() / 2.0);
        context.drawText(textRenderer, threshold, (backgroundWidth - textRenderer.getWidth(threshold)) / 2, heartsY() + 16, 0x404040, false);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!handler.remote() && sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)) return true;
        if (button == 0 && insideSettings(mouseX, mouseY)
                && mouseX >= x + listX() + listWidth() - 8) {
            draggingSettingsScroll = true;
            scrollSettingsTo(mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void positionSettings() {
        settingsScroll = Math.clamp(settingsScroll, 0, maxSettingsScroll());
        int row = y + listY() + 2 - (int) Math.round(settingsScroll);
        for (SettingsToggle setting : settings) {
            setting.setX(x + listX() + 2);
            setting.setY(row);
            row += setting.getHeight();
        }
    }
    private int maxSettingsScroll() {
        int height = 0;
        for (SettingsToggle setting : settings) height += setting.getHeight();
        return Math.max(0, height - (listHeight() - 4));
    }
    private void drawSettingsList(DrawContext context) {
        int lx = x + listX(), ly = y + listY(), right = lx + listWidth(), bottom = ly + listHeight();
        context.fill(lx, ly, right, bottom, 0xFF303030);
        context.fill(lx + 1, ly + 1, right - 1, bottom - 1, 0xFF777777);
        // Use the same textured inset and scrollbar colors as the portal frequency list.
        for (int tx = lx + 2; tx < right - 8; tx += 106)
            for (int ty = ly + 2; ty < bottom - 2; ty += 37)
                context.drawTexture(LIST_BACKGROUND, tx, ty, 0, 0,
                        Math.min(106, right - 8 - tx), Math.min(37, bottom - 2 - ty), 106, 37);
        context.fill(right - 8, ly + 1, right - 1, bottom - 1, 0xFF767676);
        context.fill(right - 7, ly + 2, right - 2, bottom - 2, 0xFF585858);
        context.fill(right - 6, ly + 2, right - 3, bottom - 2, 0xFF4B4B4B);
        context.fill(right - 2, ly + 1, right - 1, bottom - 1, 0xFF666666);
        int max = maxSettingsScroll();
        int thumbY = ly + 2 + (max == 0 ? 0 : (int) Math.round(settingsScroll * (listHeight() - 9) / max));
        context.fill(right - 7, thumbY, right - 2, thumbY + 5, 0xFF232323);
        context.fill(right - 6, thumbY + 1, right - 3, thumbY + 4, 0xFF313131);
    }
    private boolean insideSettings(double mouseX, double mouseY) {
        return mouseX >= x + listX() + 2 && mouseX < x + listX() + listWidth() - 1
                && mouseY >= y + listY() + 2 && mouseY < y + listY() + listHeight() - 2;
    }
    private void scrollSettingsTo(double mouseY) {
        settingsScroll = Math.clamp((mouseY - y - listY() - 4) * maxSettingsScroll()
                / (listHeight() - 9), 0, maxSettingsScroll());
        positionSettings();
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (insideSettings(mouseX, mouseY)) {
            settingsScroll = Math.clamp(settingsScroll - verticalAmount * 4, 0, maxSettingsScroll());
            positionSettings();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingSettingsScroll) { scrollSettingsTo(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingSettingsScroll) { draggingSettingsScroll = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private static Identifier texture(String name) {
        return Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/" + name);
    }
    private int heartsY() { return handler.remote() ? 24 : 29; }
    private int heartsX() { return (backgroundWidth - HEART_ROW_WIDTH) / 2; }
    private int listX() { return handler.remote() ? 8 : 6; }
    private int listY() { return handler.remote() ? 51 : 55; }
    private int listWidth() { return handler.remote() ? 160 : 139; }
    private int settingsTextWidth() { return (int) ((listWidth() - 39) / SETTINGS_TEXT_SCALE); }
    private int listHeight() { return handler.remote() ? 69 : 72; }
}
