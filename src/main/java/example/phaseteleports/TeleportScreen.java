package example.phaseteleports;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class TeleportScreen extends HandledScreen<TeleportScreenHandler> {
    private static final int VISIBLE_ROWS = 4;
    private TextFieldWidget nameField;
    private ButtonWidget createButton;
    private ButtonWidget setButton;
    private ButtonWidget deleteButton;
    private List<String> publicNames = List.of();
    private List<Integer> publicColors = List.of();
    private List<String> publicCreators = List.of();
    private List<String> privateNames = List.of();
    private List<Integer> privateColors = List.of();
    private List<String> privateCreators = List.of();
    private String assigned = "";
    private boolean assignedPrivate;
    private boolean assignedHidden;
    private int currentColor = PortalColors.DEFAULT;
    private String selected = "";
    private boolean privateTab;
    private boolean paletteOpen;
    private boolean confirmDelete;
    private String pendingDelete = "";
    private boolean pendingPrivate;
    private int scroll;

    public TeleportScreen(TeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 278;
    }

    private List<String> names() { return privateTab ? privateNames : publicNames; }
    private List<Integer> colors() { return privateTab ? privateColors : publicColors; }
    private List<String> creators() { return privateTab ? privateCreators : publicCreators; }

    @Override
    protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        addDrawableChild(EnergyConfigurationButton.create(x - 24, y + 6, handler));
        nameField = addDrawableChild(new TextFieldWidget(textRenderer, x + 8, y + 39, 130, 17,
                Text.translatable("gui.phaseteleports.frequency_name")));
        nameField.setMaxLength(LocalFrequencyState.MAX_NAME_LENGTH);
        createButton = addDrawableChild(ButtonWidget.builder(Text.literal("✓"), button -> createFrequency())
                .dimensions(x + 143, y + 38, 24, 19).build());
        setButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleports.set_frequency"), button -> setFrequency())
                .dimensions(x + 8, y + 160, 68, 20).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleports.delete_frequency"), button -> requestDelete())
                .dimensions(x + 81, y + 160, 64, 20).build());
        setInitialFocus(nameField);
    }

    private void send(int action, String name, int color, boolean privateFrequency) {
        ClientPlayNetworking.send(new TeleportFrequencyActionPayload(
                handler.syncId, action, name, color, privateFrequency));
    }

    private void createFrequency() {
        String name = LocalFrequencyState.normalize(nameField.getText());
        if (name.isEmpty()) return;
        selected = name;
        send(TeleportFrequencyActionPayload.CREATE, name, 0, privateTab);
        nameField.setText("");
    }

    private void setFrequency() {
        if (!selected.isEmpty() && names().contains(selected))
            send(TeleportFrequencyActionPayload.SET, selected, 0, privateTab);
    }

    private void requestDelete() {
        if (selected.isEmpty() || !names().contains(selected)) return;
        pendingDelete = selected;
        pendingPrivate = privateTab;
        paletteOpen = false;
        confirmDelete = true;
    }

    private void confirmDelete() {
        if (!confirmDelete) return;
        send(TeleportFrequencyActionPayload.DELETE, pendingDelete, 0, pendingPrivate);
        confirmDelete = false;
        pendingDelete = "";
    }

    public void applySnapshot(TeleportFrequencySnapshotPayload snapshot) {
        if (snapshot.syncId() != handler.syncId) return;
        publicNames = List.copyOf(snapshot.publicNames());
        publicColors = List.copyOf(snapshot.publicColors());
        publicCreators = List.copyOf(snapshot.publicCreators());
        privateNames = List.copyOf(snapshot.privateNames());
        privateColors = List.copyOf(snapshot.privateColors());
        privateCreators = List.copyOf(snapshot.privateCreators());
        assigned = snapshot.assigned();
        assignedPrivate = snapshot.assignedPrivate();
        assignedHidden = snapshot.assignedHidden();
        currentColor = snapshot.currentColor();
        if (!names().contains(selected)) selected = !assignedHidden && assignedPrivate == privateTab ? assigned : "";
        scroll = Math.clamp(scroll, 0, Math.max(0, names().size() - VISIBLE_ROWS));
        if (assigned.isEmpty()) paletteOpen = false;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.fill(x, y, x + 176, y + 278, 0xFF383838);
        context.fill(x + 3, y + 3, x + 173, y + 275, 0xFFC6C6C6);
        context.fill(x + 5, y + 5, x + 171, y + 273, 0xFFE0E0E0);
        context.fill(x + 5, y + 187, x + 171, y + 189, 0xFF777777);
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                drawInventorySlot(context, x + 7 + col * 18, y + 196 + row * 18);
        for (int col = 0; col < 9; col++)
            drawInventorySlot(context, x + 7 + col * 18, y + 254);
        AnchorSlotRenderer.draw(context, x - 23, y + 132, handler.getSlot(0).hasStack());
        drawTab(context, x + 8, y + 18, 77, false);
        drawTab(context, x + 89, y + 18, 78, true);
        context.fill(x + 8, y + 60, x + 168, y + 117, 0xFF686868);
        context.fill(x + 10, y + 62, x + 166, y + 115, 0xFFB4B4B4);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= names().size()) break;
            int rowY = y + 64 + row * 13;
            String name = names().get(index);
            if (name.equals(selected)) context.fill(x + 12, rowY - 1, x + 159, rowY + 12, 0xFF7775A1);
            if (index < colors().size()) context.fill(x + 15, rowY + 1, x + 24, rowY + 10,
                    0xFF000000 | PortalColors.rgb(colors().get(index)));
            String creator = index < creators().size() ? creators().get(index) : "";
            context.drawText(textRenderer, fit(name + " (" + (creator.isEmpty() ? "?" : creator) + ")", 126),
                    x + 28, rowY + 1, 0xFF202020, false);
        }
        if (names().size() > VISIBLE_ROWS) {
            int track = 48;
            int thumbY = y + 64 + scroll * (track - 10) / (names().size() - VISIBLE_ROWS);
            context.fill(x + 161, y + 64, x + 164, y + 112, 0xFF666666);
            context.fill(x + 160, thumbY, x + 165, thumbY + 10, 0xFFE0E0E0);
        }
        context.fill(x + 8, y + 121, x + 168, y + 156, 0xFFB4B4B4);
        context.fill(x + 151, y + 161, x + 168, y + 179, 0xFF4A4A4A);
        context.fill(x + 153, y + 163, x + 166, y + 177,
                assigned.isEmpty() ? 0xFF555555 : 0xFF000000 | PortalColors.rgb(currentColor));
        EnergyBarRenderer.draw(context, x - 20, y + 60, handler.getEnergy(), handler.getMaxEnergy());
    }

    private void drawTab(DrawContext context, int x, int y, int width, boolean privateType) {
        boolean active = privateTab == privateType;
        context.fill(x, y, x + width, y + 18, active ? 0xFF7775A1 : 0xFF9A9A9A);
        Text text = Text.translatable(privateType ? "gui.phaseteleports.private" : "gui.phaseteleports.public");
        context.drawCenteredTextWithShadow(textRenderer, text, x + width / 2, y + 5, 0xFFFFFFFF);
    }

    private static void drawInventorySlot(DrawContext context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, 0xFF555555);
        context.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override protected boolean isClickOutsideBounds(double mouseX, double mouseY,
            int left, int top, int button) {
        if (mouseX >= left - 24 && mouseX < left - 5
                && mouseY >= top + 131 && mouseY < top + 150) return false;
        return super.isClickOutsideBounds(mouseX, mouseY, left, top, button);
    }

    private void drawPalette(DrawContext context, int x, int y) {
        context.fill(x + 11, y + 109, x + 165, y + 154, 0xFF383838);
        context.fill(x + 13, y + 111, x + 163, y + 152, 0xFFC6C6C6);
        for (int color = 0; color < PortalColors.count(); color++) {
            int swatchX = x + 17 + (color % 8) * 18;
            int swatchY = y + 115 + (color / 8) * 18;
            context.fill(swatchX - 1, swatchY - 1, swatchX + 15, swatchY + 15,
                    color == currentColor ? 0xFFFFFFFF : 0xFF444444);
            context.fill(swatchX, swatchY, swatchX + 14, swatchY + 14,
                    0xFF000000 | PortalColors.rgb(color));
        }
    }

    private void drawDeleteConfirmation(DrawContext context, int x, int y) {
        context.fill(x + 5, y + 5, x + 171, y + 185, 0x99000000);
        context.fill(x + 9, y + 55, x + 167, y + 144, 0xFF383838);
        context.fill(x + 11, y + 57, x + 165, y + 142, 0xFFE0E0E0);
        int line = 0;
        for (var wrapped : textRenderer.wrapLines(
                Text.translatable("gui.phaseteleports.confirm_delete_question"), 140)) {
            context.drawText(textRenderer, wrapped, x + 18, y + 64 + line++ * 10, 0xFF303030, false);
        }
        line = 0;
        for (var wrapped : textRenderer.wrapLines(
                Text.translatable("gui.phaseteleports.confirm_delete_warning"), 140)) {
            context.drawText(textRenderer, wrapped, x + 18, y + 91 + line++ * 10, 0xFF9C3030, false);
        }
        context.fill(x + 19, y + 114, x + 87, y + 136, 0xFF777777);
        context.fill(x + 21, y + 116, x + 85, y + 134, 0xFFB4B4B4);
        context.fill(x + 90, y + 114, x + 158, y + 136, 0xFF777777);
        context.fill(x + 92, y + 116, x + 156, y + 134, 0xFFB4B4B4);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("gui.phaseteleports.delete_frequency"), x + 53, y + 121, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("gui.phaseteleports.cancel"), x + 124, y + 121, 0xFFFFFFFF);
    }

    private String fit(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        while (!value.isEmpty() && textRenderer.getWidth(value + "…") > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 6, 0xFF404040, false);
        Text current = assignedHidden ? Text.translatable("gui.phaseteleports.hidden_frequency")
                : assigned.isEmpty() ? Text.translatable("gui.phaseteleports.no_frequency") : Text.literal(assigned);
        context.drawText(textRenderer, Text.translatable("gui.phaseteleports.current_frequency"),
                11, 125, 0xFF404040, false);
        context.drawText(textRenderer, fit(current.getString(), 102), 64, 125, 0xFF303070, false);
        Text type = assigned.isEmpty() && !assignedHidden ? Text.literal("—")
                : Text.translatable(assignedPrivate ? "gui.phaseteleports.private" : "gui.phaseteleports.public");
        context.drawText(textRenderer, Text.translatable("gui.phaseteleports.frequency_type"),
                11, 141, 0xFF404040, false);
        context.drawText(textRenderer, type, 64, 141, 0xFF303070, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        if (nameField != null && !nameField.isMouseOver(mouseX, mouseY)) nameField.setFocused(false);
        if (confirmDelete) {
            if (button == 0 && mouseY >= y + 114 && mouseY < y + 136) {
                if (mouseX >= x + 19 && mouseX < x + 87) confirmDelete();
                else if (mouseX >= x + 90 && mouseX < x + 158) confirmDelete = false;
            }
            return true;
        }
        if (button == 0 && paletteOpen) {
            for (int color = 0; color < PortalColors.count(); color++) {
                int swatchX = x + 17 + (color % 8) * 18;
                int swatchY = y + 115 + (color / 8) * 18;
                if (mouseX >= swatchX && mouseX < swatchX + 14
                        && mouseY >= swatchY && mouseY < swatchY + 14) {
                    send(TeleportFrequencyActionPayload.COLOR, assigned, color, assignedPrivate);
                    paletteOpen = false;
                    return true;
                }
            }
            paletteOpen = false;
            if (mouseX >= x + 11 && mouseX < x + 165 && mouseY >= y + 109 && mouseY < y + 154)
                return true;
        }
        if (button == 0 && !assigned.isEmpty() && mouseX >= x + 151 && mouseX < x + 168
                && mouseY >= y + 161 && mouseY < y + 179) {
            paletteOpen = !paletteOpen;
            return true;
        }
        if (button == 0 && mouseY >= y + 18 && mouseY < y + 36) {
            if (mouseX >= x + 8 && mouseX < x + 85) { switchTab(false); return true; }
            if (mouseX >= x + 89 && mouseX < x + 167) { switchTab(true); return true; }
        }
        if (button == 0 && mouseX >= x + 10 && mouseX < x + 159
                && mouseY >= y + 63 && mouseY < y + 115) {
            int index = scroll + ((int) mouseY - y - 64) / 13;
            if (index >= 0 && index < names().size()) { selected = names().get(index); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchTab(boolean privateType) {
        privateTab = privateType;
        selected = !assignedHidden && assignedPrivate == privateTab ? assigned : "";
        scroll = 0;
        paletteOpen = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDelete) {
            if (keyCode == 256) confirmDelete = false;
            return true;
        }
        if (nameField != null && nameField.isFocused() && keyCode != 256) {
            nameField.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (confirmDelete) return true;
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        if (mouseX >= x + 10 && mouseX < x + 166 && mouseY >= y + 60 && mouseY < y + 117) {
            scroll = Math.clamp(scroll - (int) Math.signum(verticalAmount),
                    0, Math.max(0, names().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        createButton.active = !LocalFrequencyState.normalize(nameField.getText()).isEmpty();
        setButton.active = !selected.isEmpty() && names().contains(selected)
                && (assignedHidden || !selected.equals(assigned) || privateTab != assignedPrivate);
        deleteButton.active = !selected.isEmpty() && names().contains(selected);
        super.render(context, mouseX, mouseY, delta);
        if (nameField.getText().isEmpty()) {
            int x = (width - backgroundWidth) / 2;
            int y = (height - backgroundHeight) / 2;
            context.drawText(textRenderer, Text.translatable("gui.phaseteleports.frequency_name"),
                    x + 12, y + 44, 0xFF888888, false);
        }
        if (paletteOpen) drawPalette(context,
                (width - backgroundWidth) / 2, (height - backgroundHeight) / 2);
        if (confirmDelete) drawDeleteConfirmation(context,
                (width - backgroundWidth) / 2, (height - backgroundHeight) / 2);
        else {
            drawMouseoverTooltip(context, mouseX, mouseY);
            int x = (width - backgroundWidth) / 2;
            int y = (height - backgroundHeight) / 2;
            if (!handler.getSlot(0).hasStack() && mouseX >= x - 24 && mouseX < x - 5
                    && mouseY >= y + 131 && mouseY < y + 150)
                context.drawTooltip(textRenderer, Text.translatable("item.phaseteleports.anchor_upgrade"),
                        mouseX, mouseY);
            EnergyBarRenderer.drawTooltip(context, textRenderer, (width - backgroundWidth) / 2 - 20,
                    (height - backgroundHeight) / 2 + 60, mouseX, mouseY,
                    handler.getEnergy(), handler.getMaxEnergy());
        }
    }
}
