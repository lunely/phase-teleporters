package example.phaseteleports;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class PortableTeleportScreen extends HandledScreen<PortableTeleportScreenHandler> {
    private static final int VISIBLE_ROWS = 4;
    private List<PortableTeleportSnapshotPayload.Entry> localPublic = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> localPrivate = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> interdimensionalPublic = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> interdimensionalPrivate = List.of();
    private boolean privateTab;
    private boolean interdimensionalTab;
    private String selected = "";
    private int scroll;
    private ButtonWidget teleportButton;

    public PortableTeleportScreen(PortableTeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 183;
    }

    private List<PortableTeleportSnapshotPayload.Entry> entries() {
        if (interdimensionalTab) return privateTab ? interdimensionalPrivate : interdimensionalPublic;
        return privateTab ? localPrivate : localPublic;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        teleportButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleports.teleport"), button -> teleport())
                .dimensions(x + 43, y + 149, 90, 20).build());
    }

    public void applySnapshot(PortableTeleportSnapshotPayload snapshot) {
        if (snapshot.syncId() != handler.syncId) return;
        localPublic = List.copyOf(snapshot.localPublic());
        localPrivate = List.copyOf(snapshot.localPrivate());
        interdimensionalPublic = List.copyOf(snapshot.interdimensionalPublic());
        interdimensionalPrivate = List.copyOf(snapshot.interdimensionalPrivate());
        if (entries().stream().noneMatch(entry -> entry.name().equals(selected))) selected = "";
        scroll = Math.clamp(scroll, 0, Math.max(0, entries().size() - VISIBLE_ROWS));
    }

    private void teleport() {
        if (selected.isEmpty() || entries().stream().noneMatch(entry -> entry.name().equals(selected))) return;
        ClientPlayNetworking.send(new PortableTeleportActionPayload(
                handler.syncId, interdimensionalTab, privateTab, selected));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.fill(x, y, x + 176, y + 183, 0xFF383838);
        context.fill(x + 3, y + 3, x + 173, y + 180, 0xFFC6C6C6);
        context.fill(x + 5, y + 5, x + 171, y + 178, 0xFFE0E0E0);
        drawTab(context, x + 8, y + 20, 77, !privateTab,
                Text.translatable("gui.phaseteleports.public"));
        drawTab(context, x + 89, y + 20, 78, privateTab,
                Text.translatable("gui.phaseteleports.private"));
        drawTab(context, x + 8, y + 44, 77, !interdimensionalTab,
                Text.translatable("gui.phaseteleports.local"));
        drawTab(context, x + 89, y + 44, 78, interdimensionalTab,
                Text.translatable("gui.phaseteleports.interdimensional"));
        context.fill(x + 8, y + 69, x + 168, y + 133, 0xFF686868);
        context.fill(x + 10, y + 71, x + 166, y + 131, 0xFFB4B4B4);
        List<PortableTeleportSnapshotPayload.Entry> visible = entries();
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= visible.size()) break;
            int rowY = y + 75 + row * 13;
            PortableTeleportSnapshotPayload.Entry entry = visible.get(index);
            if (entry.name().equals(selected))
                context.fill(x + 12, rowY - 1, x + 159, rowY + 12, 0xFF7775A1);
            context.fill(x + 15, rowY + 1, x + 24, rowY + 10,
                    0xFF000000 | PortalColors.rgb(entry.color()));
            String creator = entry.creator().isEmpty() ? "?" : entry.creator();
            context.drawText(textRenderer, fit(entry.name() + " (" + creator + ")", 126),
                    x + 28, rowY + 1, 0xFF202020, false);
        }
        if (visible.size() > VISIBLE_ROWS) {
            int thumbY = y + 75 + scroll * 43 / (visible.size() - VISIBLE_ROWS);
            context.fill(x + 161, y + 75, x + 164, y + 128, 0xFF666666);
            context.fill(x + 160, thumbY, x + 165, thumbY + 10, 0xFFE0E0E0);
        }
    }

    private void drawTab(DrawContext context, int x, int y, int tabWidth, boolean active, Text label) {
        context.fill(x, y, x + tabWidth, y + 18, active ? 0xFF7775A1 : 0xFF9A9A9A);
        float scale = Math.min(1.0f, (float) (tabWidth - 8) / Math.max(1, textRenderer.getWidth(label)));
        context.getMatrices().push();
        context.getMatrices().translate(x + tabWidth / 2.0, y + 5, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawCenteredTextWithShadow(textRenderer, label, 0, 0, 0xFFFFFFFF);
        context.getMatrices().pop();
    }

    private String fit(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        while (!value.isEmpty() && textRenderer.getWidth(value + "…") > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 8, 7, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        if (button == 0 && mouseY >= y + 20 && mouseY < y + 38) {
            if (mouseX >= x + 8 && mouseX < x + 85) { switchPrivate(false); return true; }
            if (mouseX >= x + 89 && mouseX < x + 167) { switchPrivate(true); return true; }
        }
        if (button == 0 && mouseY >= y + 44 && mouseY < y + 62) {
            if (mouseX >= x + 8 && mouseX < x + 85) { switchType(false); return true; }
            if (mouseX >= x + 89 && mouseX < x + 167) { switchType(true); return true; }
        }
        if (button == 0 && mouseX >= x + 10 && mouseX < x + 159
                && mouseY >= y + 75 && mouseY < y + 127) {
            int index = scroll + ((int) mouseY - y - 75) / 13;
            if (index < entries().size()) {
                selected = entries().get(index).name();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void switchPrivate(boolean value) {
        if (privateTab == value) return;
        privateTab = value;
        selected = "";
        scroll = 0;
    }

    private void switchType(boolean value) {
        if (interdimensionalTab == value) return;
        interdimensionalTab = value;
        selected = "";
        scroll = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        if (mouseX >= x + 10 && mouseX < x + 166 && mouseY >= y + 69 && mouseY < y + 133) {
            scroll = Math.clamp(scroll - (int) Math.signum(verticalAmount),
                    0, Math.max(0, entries().size() - VISIBLE_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        teleportButton.active = !selected.isEmpty()
                && entries().stream().anyMatch(entry -> entry.name().equals(selected));
        super.render(context, mouseX, mouseY, delta);
    }
}
