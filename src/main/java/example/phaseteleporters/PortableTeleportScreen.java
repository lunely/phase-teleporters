package example.phaseteleporters;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Portable teleporter uses the same frequency controls without machine panels. */
public final class PortableTeleportScreen extends HandledScreen<PortableTeleportScreenHandler> {
    private static final Identifier BACKGROUND = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/teleporter_gui.png");
    private static final Identifier PUBLIC_SLOT = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/slot/public_slot.png");
    private static final Identifier PRIVATE_LOCK = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/slot/private_lock.png");
    private static final Identifier LOCAL_SLOT = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/slot/local_slot.png");
    private static final Identifier INTERDIMENSIONAL_SLOT = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/slot/interdimensional_slot.png");
    private static final Identifier LIST_BACKGROUND = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/gui/frequency_list_background.png");
    private static final int LIST_X = 29;
    private static final int LIST_Y = 74;
    private static final int LIST_WIDTH = 116;
    private static final int LIST_HEIGHT = 41;
    private static final int TYPE_X = LIST_X + LIST_WIDTH + 2;
    private static final int TYPE_SIZE = 20;
    private static final int TYPE_GAP = 1;
    private static final int ROW_HEIGHT = 11;
    private static final int VIEW_HEIGHT = LIST_HEIGHT - 4;
    private static final int THUMB_TRAVEL = VIEW_HEIGHT - 5;
    private static final int INPUT_Y = 116;
    private static final int INPUT_HEIGHT = 14;
    private static final int CHECK_X = LIST_X + LIST_WIDTH - 12;
    private static final int CHECK_Y = INPUT_Y + 1;
    private static final int CHECK_SIZE = 11;
    private static final int ACTION_Y = 132;
    private static final float LIST_TEXT_SCALE = 0.8f;

    private List<PortableTeleportSnapshotPayload.Entry> localPublic = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> localPrivate = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> interdimensionalPublic = List.of();
    private List<PortableTeleportSnapshotPayload.Entry> interdimensionalPrivate = List.of();
    private boolean privateMode;
    private boolean interdimensionalMode;
    private boolean receivedSnapshot;
    private boolean draggingScroll;
    private String selected = "";
    private boolean selectedPrivate;
    private boolean selectedInterdimensional;
    private String rememberedName = "";
    private boolean rememberedPrivate;
    private boolean rememberedInterdimensional;
    private double scrollPixels;
    private ButtonWidget localButton;
    private ButtonWidget interdimensionalButton;
    private ButtonWidget publicButton;
    private ButtonWidget privateButton;
    private ButtonWidget teleportButton;
    private ButtonWidget deleteButton;
    private TextFieldWidget frequencyField;

    public PortableTeleportScreen(PortableTeleportScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 175;
        backgroundHeight = 237;
    }

    private List<PortableTeleportSnapshotPayload.Entry> entries() {
        return entriesFor(privateMode, interdimensionalMode);
    }

    private List<PortableTeleportSnapshotPayload.Entry> entriesFor(
            boolean privateFrequency, boolean interdimensional) {
        if (interdimensional)
            return privateFrequency ? interdimensionalPrivate : interdimensionalPublic;
        return privateFrequency ? localPrivate : localPublic;
    }

    private PortableTeleportSnapshotPayload.Entry selectedEntry() {
        for (PortableTeleportSnapshotPayload.Entry entry : entriesFor(selectedPrivate, selectedInterdimensional))
            if (entry.name().equals(selected)) return entry;
        return null;
    }

    @Override
    protected void init() {
        super.init();
        localButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> switchType(false))
                .dimensions(x + TYPE_X, y + LIST_Y, TYPE_SIZE, TYPE_SIZE).build());
        interdimensionalButton = addDrawableChild(ButtonWidget.builder(Text.empty(),
                button -> switchType(true))
                .dimensions(x + TYPE_X, y + LIST_Y + TYPE_SIZE + TYPE_GAP,
                        TYPE_SIZE, TYPE_SIZE).build());
        publicButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> switchPrivate(false))
                .dimensions(x + 7, y + LIST_Y, 20, 20).build());
        privateButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> switchPrivate(true))
                .dimensions(x + 7, y + LIST_Y + 21, 20, 20).build());
        teleportButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleporters.teleport"), button -> teleport())
                .dimensions(x + 29, y + ACTION_Y, 56, 18).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleporters.delete_frequency"), button -> deleteFrequency())
                .dimensions(x + 89, y + ACTION_Y, 56, 18).build());
        frequencyField = addDrawableChild(new TextFieldWidget(textRenderer,
                x + LIST_X + 3, y + INPUT_Y + 2, LIST_WIDTH - 18, 10,
                Text.translatable("gui.phaseteleporters.frequency_name")));
        frequencyField.setMaxLength(LocalFrequencyState.MAX_NAME_LENGTH);
        frequencyField.setDrawsBackground(false);
        updateButtons();
        ClientPlayNetworking.send(new PortableSnapshotRequestPayload(handler.syncId));
    }

    public void applySnapshot(PortableTeleportSnapshotPayload snapshot) {
        if (snapshot.syncId() != handler.syncId) return;
        rememberedName = snapshot.selectedName();
        rememberedPrivate = snapshot.selectedPrivate();
        rememberedInterdimensional = snapshot.selectedInterdimensional();
        if (!receivedSnapshot) {
            privateMode = snapshot.viewPrivate();
            interdimensionalMode = snapshot.viewInterdimensional();
            selected = rememberedName;
            selectedPrivate = rememberedPrivate;
            selectedInterdimensional = rememberedInterdimensional;
            receivedSnapshot = true;
        }
        localPublic = List.copyOf(snapshot.localPublic());
        localPrivate = List.copyOf(snapshot.localPrivate());
        interdimensionalPublic = List.copyOf(snapshot.interdimensionalPublic());
        interdimensionalPrivate = List.copyOf(snapshot.interdimensionalPrivate());
        if (!selected.isEmpty() && entriesFor(selectedPrivate, selectedInterdimensional).stream()
                .noneMatch(entry -> entry.name().equals(selected))) selected = "";
        scrollPixels = Math.clamp(scrollPixels, 0, maxScrollPixels());
        updateButtons();
    }

    private void updateButtons() {
        if (localButton != null) localButton.active = interdimensionalMode;
        if (interdimensionalButton != null) interdimensionalButton.active = !interdimensionalMode;
        if (publicButton != null) publicButton.active = privateMode;
        if (privateButton != null) privateButton.active = !privateMode;
        boolean hasSelection = selectedEntry() != null;
        if (teleportButton != null) teleportButton.active = hasSelection;
        if (deleteButton != null) deleteButton.active = hasSelection
                && selectedPrivate == privateMode && selectedInterdimensional == interdimensionalMode;
    }

    private void switchPrivate(boolean value) {
        if (privateMode == value) return;
        privateMode = value;
        switchView();
    }

    private void switchType(boolean value) {
        if (interdimensionalMode == value) return;
        interdimensionalMode = value;
        switchView();
    }

    private void switchView() {
        scrollPixels = 0;
        selected = rememberedName;
        selectedPrivate = rememberedPrivate;
        selectedInterdimensional = rememberedInterdimensional;
        saveView();
        updateButtons();
    }

    private void saveView() {
        ClientPlayNetworking.send(new PortableSelectionPayload(handler.syncId,
                interdimensionalMode, privateMode));
    }

    private void sendFrequencyAction(int action, String name) {
        ClientPlayNetworking.send(new PortableFrequencyActionPayload(handler.syncId, action,
                interdimensionalMode, privateMode, name));
    }

    private void createFrequency() {
        if (frequencyField == null) return;
        String name = LocalFrequencyState.normalize(frequencyField.getText());
        if (name.isEmpty() || entries().stream().anyMatch(entry -> entry.name().equals(name))) return;
        sendFrequencyAction(PortableFrequencyActionPayload.CREATE, name);
        selected = name;
        selectedPrivate = privateMode;
        selectedInterdimensional = interdimensionalMode;
        frequencyField.setText("");
        frequencyField.setFocused(false);
        playClick();
        updateButtons();
    }

    private void deleteFrequency() {
        if (selectedEntry() == null || selectedPrivate != privateMode
                || selectedInterdimensional != interdimensionalMode) return;
        sendFrequencyAction(PortableFrequencyActionPayload.DELETE, selected);
        selected = "";
        updateButtons();
    }

    private void teleport() {
        if (selectedEntry() == null) return;
        ClientPlayNetworking.send(new PortableTeleportActionPayload(
                handler.syncId, selectedInterdimensional, selectedPrivate, selected));
    }

    private int maxScrollPixels() {
        return Math.max(0, entries().size() * ROW_HEIGHT - VIEW_HEIGHT);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(BACKGROUND, x, y, 5, 2, backgroundWidth, backgroundHeight, 256, 256);
        // Keep the shared inventory art while hiding machine-only controls.
        context.fill(x + 7, y + 4, x + 21, y + 19, 0xFFC6C6C6);
        context.fill(x + 151, y + 78, x + 170, y + 151, 0xFFC6C6C6);
        context.fill(x + 7, y + 130, x + 29, y + 152, 0xFFC6C6C6);
        drawFrequencyList(context);
        drawFrequencyInput(context, mouseX, mouseY);
    }

    private void drawFrequencyList(DrawContext context) {
        int lx = x + LIST_X;
        int ly = y + LIST_Y;
        int contentRight = lx + LIST_WIDTH - 8;
        int trackLeft = contentRight;
        context.fill(lx, ly, lx + LIST_WIDTH, ly + LIST_HEIGHT, 0xFF303030);
        context.fill(lx + 1, ly + 1, lx + LIST_WIDTH - 1, ly + LIST_HEIGHT - 1, 0xFF777777);
        context.drawTexture(LIST_BACKGROUND, lx + 2, ly + 2,
                0, 0, 106, VIEW_HEIGHT, 106, VIEW_HEIGHT);
        context.fill(trackLeft, ly + 1, lx + LIST_WIDTH - 1, ly + LIST_HEIGHT - 1, 0xFF767676);
        context.fill(trackLeft + 1, ly + 2, lx + LIST_WIDTH - 2, ly + LIST_HEIGHT - 2, 0xFF585858);
        context.fill(trackLeft + 2, ly + 2, lx + LIST_WIDTH - 3, ly + LIST_HEIGHT - 2, 0xFF4B4B4B);
        List<PortableTeleportSnapshotPayload.Entry> visible = entries();
        int offset = (int) Math.round(scrollPixels);
        int firstIndex = Math.max(0, offset / ROW_HEIGHT);
        context.enableScissor(lx + 2, ly + 2, contentRight, ly + LIST_HEIGHT - 2);
        for (int index = firstIndex; index < visible.size(); index++) {
            PortableTeleportSnapshotPayload.Entry entry = visible.get(index);
            int rowY = ly + 2 + index * ROW_HEIGHT - offset;
            if (rowY >= ly + LIST_HEIGHT - 2) break;
            if (selectedPrivate == privateMode && selectedInterdimensional == interdimensionalMode
                    && entry.name().equals(selected))
                context.fill(lx + 2, rowY - 1, contentRight, rowY + ROW_HEIGHT - 1, 0xFF285E60);
            String label = entry.creator().isEmpty() ? entry.name()
                    : entry.name() + " (" + entry.creator() + ")";
            label = fit(label, (int) ((LIST_WIDTH - 15) / LIST_TEXT_SCALE));
            context.getMatrices().push();
            context.getMatrices().translate(lx + 3, rowY + 1, 0);
            context.getMatrices().scale(LIST_TEXT_SCALE, LIST_TEXT_SCALE, 1);
            context.drawText(textRenderer, label, 0, 0, 0xFF39FF39, false);
            context.getMatrices().pop();
        }
        context.disableScissor();
        int maxScroll = maxScrollPixels();
        int thumbY = ly + 2 + (maxScroll == 0 ? 0
                : (int) Math.round(scrollPixels * THUMB_TRAVEL / maxScroll));
        context.fill(trackLeft + 1, thumbY, trackLeft + 6, thumbY + 5, 0xFF232323);
        context.fill(trackLeft + 2, thumbY + 1, trackLeft + 5, thumbY + 4, 0xFF313131);
    }

    private void drawFrequencyInput(DrawContext context, int mouseX, int mouseY) {
        int fx = x + LIST_X;
        int fy = y + INPUT_Y;
        context.fill(fx, fy, fx + LIST_WIDTH, fy + INPUT_HEIGHT, 0xFF343434);
        context.fill(fx + 1, fy + 1, fx + LIST_WIDTH - 1, fy + INPUT_HEIGHT - 1, 0xFF171717);
        if (frequencyField != null && frequencyField.getText().isEmpty())
            context.drawText(textRenderer, Text.translatable("gui.phaseteleporters.frequency_name"),
                    fx + 3, fy + 3, 0xFF777777, false);
        boolean enabled = frequencyField != null
                && !LocalFrequencyState.normalize(frequencyField.getText()).isEmpty()
                && entries().stream().noneMatch(entry -> entry.name().equals(
                        LocalFrequencyState.normalize(frequencyField.getText())));
        boolean hovered = inside(mouseX, mouseY, x + CHECK_X, y + CHECK_Y, CHECK_SIZE, CHECK_SIZE);
        int checkU = enabled ? (hovered ? 203 : 189) : 217;
        context.drawTexture(BACKGROUND, x + CHECK_X, y + CHECK_Y,
                checkU, 23, CHECK_SIZE, CHECK_SIZE, 256, 256);
        if (!enabled)
            context.fill(x + CHECK_X, y + CHECK_Y,
                    x + CHECK_X + CHECK_SIZE, y + CHECK_Y + CHECK_SIZE, 0x66000000);
    }

    private void drawInfo(DrawContext context, int rowY, String key, String value, int valueColor) {
        Text label = Text.translatable(key);
        int labelWidth = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, LIST_X, rowY, 0xFF404040, false);
        int valueX = LIST_X + labelWidth + 3;
        int remaining = LIST_X + LIST_WIDTH - valueX;
        if (remaining > 0)
            context.drawText(textRenderer, fit(value, remaining), valueX, rowY, valueColor, false);
    }

    private String fit(String value, int maxWidth) {
        if (textRenderer.getWidth(value) <= maxWidth) return value;
        while (!value.isEmpty() && textRenderer.getWidth(value + "…") > maxWidth)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    private boolean inside(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    private void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        Text heading = title;
        int titleWidth = Math.max(1, textRenderer.getWidth(heading));
        float scale = Math.min(1.0f, (float) (LIST_WIDTH - 4) / titleWidth);
        context.getMatrices().push();
        context.getMatrices().translate(LIST_X + LIST_WIDTH / 2.0, 5, 0);
        context.getMatrices().scale(scale, scale, 1);
        context.drawText(textRenderer, heading, -titleWidth / 2, 0, 0xFF404040, false);
        context.getMatrices().pop();
        PortableTeleportSnapshotPayload.Entry entry = selectedEntry();
        drawInfo(context, 42, "gui.phaseteleporters.info.frequency",
                entry == null ? Text.translatable("gui.phaseteleporters.no_frequency").getString()
                        : entry.name(), entry == null ? 0xFF777777 : 0xFF404040);
        drawInfo(context, 52, "gui.phaseteleporters.info.owner",
                entry == null || entry.creator().isEmpty() ? "—" : entry.creator(),
                entry == null || entry.creator().isEmpty() ? 0xFF777777 : 0xFF39FF39);
        drawInfo(context, 62, "gui.phaseteleporters.info.security",
                entry == null ? "—" : Text.translatable(selectedPrivate
                        ? "gui.phaseteleporters.private" : "gui.phaseteleporters.public").getString(),
                entry == null ? 0xFF777777 : 0xFF39FF39);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        updateButtons();
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient.getInstance().getTextureManager().getTexture(PUBLIC_SLOT).setFilter(false, false);
        MinecraftClient.getInstance().getTextureManager().getTexture(LOCAL_SLOT).setFilter(false, false);
        MinecraftClient.getInstance().getTextureManager().getTexture(INTERDIMENSIONAL_SLOT)
                .setFilter(false, false);
        context.drawTexture(PUBLIC_SLOT, x + 12, y + LIST_Y + 5,
                36, 19, 9, 9, 256, 256);
        context.drawTexture(PRIVATE_LOCK, x + 13, y + LIST_Y + 26,
                51, 19, 7, 9, 256, 256);
        context.drawTexture(LOCAL_SLOT, x + TYPE_X + 4, y + LIST_Y + 4,
                74, 56, 11, 11, 256, 256);
        context.drawTexture(INTERDIMENSIONAL_SLOT,
                x + TYPE_X + 3, y + LIST_Y + TYPE_SIZE + TYPE_GAP + 3,
                112, 60, 13, 13, 256, 256);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (inside(mouseX, mouseY, x + 7, y + LIST_Y, 20, 20))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.show_public_frequencies"), mouseX, mouseY);
        else if (inside(mouseX, mouseY, x + 7, y + LIST_Y + 21, 20, 20))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.show_private_frequencies"), mouseX, mouseY);
        else if (inside(mouseX, mouseY, x + TYPE_X, y + LIST_Y, TYPE_SIZE, TYPE_SIZE))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.local"), mouseX, mouseY);
        else if (inside(mouseX, mouseY, x + TYPE_X,
                y + LIST_Y + TYPE_SIZE + TYPE_GAP, TYPE_SIZE, TYPE_SIZE))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.interdimensional"), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, x + CHECK_X, y + CHECK_Y,
                CHECK_SIZE, CHECK_SIZE)) {
            createFrequency();
            return true;
        }
        if (button == 0 && inside(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            int trackLeft = x + LIST_X + LIST_WIDTH - 8;
            if (mouseX >= trackLeft) {
                draggingScroll = true;
                scrollTo(mouseY);
            } else if (mouseY >= y + LIST_Y + 2 && mouseY < y + LIST_Y + LIST_HEIGHT - 2) {
                int index = (int) Math.floor((mouseY - y - LIST_Y - 2 + scrollPixels) / ROW_HEIGHT);
                if (index >= 0 && index < entries().size()) {
                    PortableTeleportSnapshotPayload.Entry entry = entries().get(index);
                    selected = entry.name();
                    selectedPrivate = privateMode;
                    selectedInterdimensional = interdimensionalMode;
                    if (frequencyField != null) frequencyField.setFocused(false);
                    updateButtons();
                    playClick();
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void scrollTo(double mouseY) {
        int maxScroll = maxScrollPixels();
        scrollPixels = Math.clamp((mouseY - y - LIST_Y - 4) * maxScroll / THUMB_TRAVEL,
                0.0, maxScroll);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double deltaX, double deltaY) {
        if (button == 0 && draggingScroll) {
            scrollTo(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
            double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            scrollPixels = Math.clamp(scrollPixels - verticalAmount * 8.0, 0, maxScrollPixels());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (frequencyField != null && frequencyField.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                createFrequency();
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                frequencyField.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (frequencyField != null && frequencyField.isFocused())
            return frequencyField.charTyped(chr, modifiers);
        return super.charTyped(chr, modifiers);
    }
}
