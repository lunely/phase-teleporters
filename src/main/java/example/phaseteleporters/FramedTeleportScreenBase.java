package example.phaseteleporters;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/** Current staged layout shared by both framed teleporters. */
abstract class FramedTeleportScreenBase<T extends ScreenHandler & EnergySideScreenHandler>
        extends HandledScreen<T> {
    private static final Identifier BACKGROUND = texture("textures/gui/teleporter_gui.png");
    private static final Identifier COLOR_PANEL = texture("textures/gui/color_config_menu.png");
    private static final Identifier COLOR_BUTTON = texture("textures/gui/slot/config_button.png");
    private static final Identifier COLOR_SLOT = texture("textures/gui/color_slot.png");
    private static final Identifier COLOR_BUTTON_ICON = texture("textures/gui/slot/color_slot.png");
    private static final Identifier PUBLIC_SLOT = texture("textures/gui/slot/public_slot.png");
    private static final Identifier PRIVATE_LOCK = texture("textures/gui/slot/private_lock.png");
    private static final Identifier LIST_BACKGROUND = texture("textures/gui/frequency_list_background.png");
    private static final Identifier EMPTY_ENERGY = texture("textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY = texture("textures/gui/electric_furnace_energy_filled.png");
    private static final Identifier ENERGY_SLOT_ICON = texture("textures/gui/slot/energy_slot.png");
    private static final Identifier CHUNKLOADER_SLOT_ICON = texture("textures/gui/slot/chunkloader_slot.png");
    private static final int PANEL_WIDTH = 135;
    private static final int PANEL_HEIGHT = 132;
    private static final int COLOR_SLOT_SIZE = 18;
    private static final int COLOR_COLUMNS = 5;
    private static final int COLOR_STEP = 24;
    private static final int BUTTON_WIDTH = 25;
    private static final int BUTTON_HEIGHT = 22;
    private static final int ACTION_Y = 126;
    private static final int TAB_X = 7;
    private static final int TAB_Y = 69;
    private static final int TAB_SIZE = 20;
    private static final int TAB_GAP = 1;
    private static final int STATUS_X = TAB_X;
    private static final int STATUS_Y = 5;
    private static final int STATUS_SIZE = 13;
    private static final int LIST_X = 29;
    private static final int LIST_Y = 69;
    private static final int LIST_WIDTH = 116;
    private static final int LIST_HEIGHT = 41;
    private static final int ROW_HEIGHT = 11;
    private static final float LIST_TEXT_SCALE = 0.8f;
    private static final int LIST_VIEW_HEIGHT = LIST_HEIGHT - 4;
    private static final int THUMB_TRAVEL = LIST_VIEW_HEIGHT - 5;
    private static final int INPUT_X = 29;
    private static final int INPUT_Y = 111;
    private static final int INPUT_WIDTH = 116;
    private static final int INPUT_HEIGHT = 14;
    private static final int CHECK_X = INPUT_X + INPUT_WIDTH - 12;
    private static final int CHECK_Y = INPUT_Y + 1;
    private static final int CHECK_SIZE = 11;
    private static final int ENERGY_X = 152;
    private static final int ENERGY_Y = 100;
    private static final int ENERGY_WIDTH = 16;
    private static final int ENERGY_HEIGHT = 48;
    private static final int ENERGY_ITEM_X = 152;
    private static final int ENERGY_ITEM_Y = 79;
    private static final int CHUNKLOADER_X = 8;
    private static final int CHUNKLOADER_Y = 132;
    private static final int CREATE_ACTION = 0;
    private static final int SET_ACTION = 1;
    private static final int DELETE_ACTION = 3;

    private record FrequencyEntry(String name, int color, String creator) {}

    private boolean privateTab;
    private boolean colorOpen;
    private int color = PortalColors.DEFAULT;
    private String assigned = "";
    private boolean assignedPrivate;
    private boolean receivedSnapshot;
    private List<FrequencyEntry> publicFrequencies = List.of();
    private List<FrequencyEntry> privateFrequencies = List.of();
    private String selected = "";
    private double scrollPixels;
    private boolean draggingScroll;
    private ButtonWidget publicButton;
    private ButtonWidget privateButton;
    private ButtonWidget setButton;
    private ButtonWidget deleteButton;
    private TextFieldWidget frequencyField;
    private final EnergySidePanel sidePanel = new EnergySidePanel();

    protected FramedTeleportScreenBase(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 175;
        backgroundHeight = 237;
    }

    private static Identifier texture(String path) {
        return Identifier.of(PhaseTeleportersMod.MOD_ID, path);
    }

    @Override
    protected void init() {
        super.init();
        updateColorPanelLayout();
        int x = this.x;
        int y = this.y;
        publicButton = addDrawableChild(ButtonWidget.builder(
                Text.empty(), button -> selectTab(false))
                .dimensions(x + TAB_X, y + TAB_Y, TAB_SIZE, TAB_SIZE).build());
        privateButton = addDrawableChild(ButtonWidget.builder(
                Text.empty(), button -> selectTab(true))
                .dimensions(x + TAB_X, y + TAB_Y + TAB_SIZE + TAB_GAP, TAB_SIZE, TAB_SIZE).build());
        setButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleporters.set_frequency"), button -> useSelection(false))
                .dimensions(x + 29, y + ACTION_Y, 56, 20).build());
        deleteButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.phaseteleporters.delete_frequency"), button -> useSelection(true))
                .dimensions(x + 89, y + ACTION_Y, 56, 20).build());
        frequencyField = addDrawableChild(new TextFieldWidget(textRenderer,
                x + INPUT_X + 3, y + INPUT_Y + 2, INPUT_WIDTH - 18, 10,
                Text.translatable("gui.phaseteleporters.frequency_name")));
        frequencyField.setMaxLength(LocalFrequencyState.MAX_NAME_LENGTH);
        frequencyField.setDrawsBackground(false);
        frequencyField.setChangedListener(value -> updateButtons());
        updateTabs();
        updateButtons();
    }

    private void selectTab(boolean value) {
        privateTab = value;
        selected = "";
        scrollPixels = 0;
        updateTabs();
        updateButtons();
    }

    private void updateTabs() {
        if (publicButton != null) publicButton.active = privateTab;
        if (privateButton != null) privateButton.active = !privateTab;
    }

    protected void applyFrequencySnapshot(String name, boolean isPrivate, int currentColor) {
        assigned = name;
        assignedPrivate = isPrivate;
        if (PortalColors.isValid(currentColor)) color = currentColor;
    }

    protected void applyFrequencySnapshot(String name, boolean isPrivate, int currentColor,
            List<String> publicNames, List<Integer> publicColors, List<String> publicCreators,
            List<String> privateNames, List<Integer> privateColors, List<String> privateCreators) {
        applyFrequencySnapshot(name, isPrivate, currentColor);
        publicFrequencies = entries(publicNames, publicColors, publicCreators);
        privateFrequencies = entries(privateNames, privateColors, privateCreators);
        if (!receivedSnapshot && !name.isEmpty()) {
            privateTab = isPrivate;
            selected = name;
            updateTabs();
        }
        receivedSnapshot = true;
        if (selected.isEmpty() && !name.isEmpty() && privateTab == isPrivate) selected = name;
        if (frequencies().stream().noneMatch(entry -> entry.name().equals(selected))) selected = "";
        scrollPixels = Math.clamp(scrollPixels, 0, maxScrollPixels());
        updateButtons();
    }

    private static List<FrequencyEntry> entries(List<String> names, List<Integer> colors,
            List<String> creators) {
        List<FrequencyEntry> entries = new ArrayList<>();
        for (int i = 0; i < names.size() && i < colors.size() && i < creators.size(); i++)
            entries.add(new FrequencyEntry(names.get(i), colors.get(i), creators.get(i)));
        return List.copyOf(entries);
    }

    private List<FrequencyEntry> frequencies() {
        return privateTab ? privateFrequencies : publicFrequencies;
    }

    private int maxScrollPixels() {
        return Math.max(0, frequencies().size() * ROW_HEIGHT - LIST_VIEW_HEIGHT);
    }

    private void updateButtons() {
        boolean hasSelection = !selected.isEmpty()
                && frequencies().stream().anyMatch(entry -> entry.name().equals(selected));
        if (setButton != null) setButton.active = hasSelection
                && !(selected.equals(assigned) && privateTab == assignedPrivate);
        if (deleteButton != null) deleteButton.active = hasSelection;
    }

    private void useSelection(boolean delete) {
        if (selected.isEmpty()) return;
        if (!delete && selected.equals(assigned) && privateTab == assignedPrivate) return;
        sendFrequencyAction(delete ? DELETE_ACTION : SET_ACTION, selected, color, privateTab);
        if (delete) selected = "";
        updateButtons();
    }

    private void createFrequency() {
        if (frequencyField == null) return;
        String name = LocalFrequencyState.normalize(frequencyField.getText());
        if (name.isEmpty() || frequencies().stream().anyMatch(entry -> entry.name().equals(name))) return;
        sendFrequencyAction(CREATE_ACTION, name, color, privateTab);
        selected = name;
        frequencyField.setText("");
        frequencyField.setFocused(false);
        updateButtons();
        playClick();
    }

    protected abstract void sendColor(String name, boolean isPrivate, int color);
    protected abstract void sendFrequencyAction(int action, String name, int color, boolean isPrivate);
    protected abstract Text guiTitle();
    protected abstract int getPortalStatus();
    protected boolean showsPortalStatus() { return true; }
    protected boolean showsColorConfiguration() { return true; }
    protected abstract long getEnergy();
    protected abstract long getMaxEnergy();

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        if (colorOpen) drawColorPanel(context, mouseX, mouseY);
        if (showsColorConfiguration() && !colorOpen) {
            int bx = colorButtonX();
            int by = colorButtonY();
            context.drawTexture(COLOR_BUTTON, bx, by, 7, 5, BUTTON_WIDTH, BUTTON_HEIGHT, 256, 256);
            context.drawTexture(COLOR_BUTTON_ICON, bx + 5, by + 5,
                    12, 10, 15, 12, 256, 256);
        }
        context.drawTexture(BACKGROUND, x, y, 5, 2, backgroundWidth, backgroundHeight, 256, 256);
        if (showsPortalStatus()) context.drawTexture(BACKGROUND, x + STATUS_X, y + STATUS_Y,
                getPortalStatus() == PortalStatus.ACTIVE ? 208 : 192, 2,
                STATUS_SIZE, STATUS_SIZE, 256, 256);
        drawEnergy(context);
        if (handler.getSlot(0).getStack().isEmpty()) {
            MinecraftClient.getInstance().getTextureManager().getTexture(CHUNKLOADER_SLOT_ICON)
                    .setFilter(false, false);
            context.drawTexture(CHUNKLOADER_SLOT_ICON, x + CHUNKLOADER_X, y + CHUNKLOADER_Y,
                    16, 16, 0, 0, 32, 32, 32, 32);
        }
        if (handler.getSlot(1).getStack().isEmpty())
            context.drawTexture(ENERGY_SLOT_ICON, x + ENERGY_ITEM_X, y + ENERGY_ITEM_Y,
                    0, 0, 16, 16, 16, 16);
        drawFrequencyList(context);
        drawFrequencyInput(context, mouseX, mouseY);
    }

    private void drawEnergy(DrawContext context) {
        int left = x + ENERGY_X;
        int top = y + ENERGY_Y;
        int bottom = top + ENERGY_HEIGHT;
        context.fill(left - 1, top - 1, left + ENERGY_WIDTH + 1, bottom + 1, 0xFF373737);
        context.drawTexture(EMPTY_ENERGY, left, top, 0, 0,
                ENERGY_WIDTH, ENERGY_HEIGHT, ENERGY_WIDTH, ENERGY_HEIGHT);
        long capacity = getMaxEnergy();
        if (capacity <= 0) return;
        int filled = (int) Math.min(ENERGY_HEIGHT,
                Math.max(0, getEnergy()) * ENERGY_HEIGHT / capacity);
        if (filled <= 0) return;
        context.drawTexture(FILLED_ENERGY, left, bottom - filled,
                0, ENERGY_HEIGHT - filled, ENERGY_WIDTH, filled,
                ENERGY_WIDTH, ENERGY_HEIGHT);
    }

    private void drawFrequencyList(DrawContext context) {
        int lx = x + LIST_X;
        int ly = y + LIST_Y;
        int contentRight = lx + LIST_WIDTH - 8;
        int trackLeft = contentRight;
        context.fill(lx, ly, lx + LIST_WIDTH, ly + LIST_HEIGHT, 0xFF303030);
        context.fill(lx + 1, ly + 1, lx + LIST_WIDTH - 1, ly + LIST_HEIGHT - 1, 0xFF777777);
        context.drawTexture(LIST_BACKGROUND, lx + 2, ly + 2,
                0, 0, 106, LIST_VIEW_HEIGHT, 106, LIST_VIEW_HEIGHT);
        context.fill(trackLeft, ly + 1, lx + LIST_WIDTH - 1,
                ly + LIST_HEIGHT - 1, 0xFF767676);
        context.fill(trackLeft + 1, ly + 2, lx + LIST_WIDTH - 2,
                ly + LIST_HEIGHT - 2, 0xFF585858);
        context.fill(trackLeft + 2, ly + 2, lx + LIST_WIDTH - 3,
                ly + LIST_HEIGHT - 2, 0xFF4B4B4B);
        context.fill(lx + LIST_WIDTH - 2, ly + 1, lx + LIST_WIDTH - 1,
                ly + LIST_HEIGHT - 1, 0xFF666666);
        List<FrequencyEntry> entries = frequencies();
        int offset = (int) Math.round(scrollPixels);
        int firstIndex = Math.max(0, offset / ROW_HEIGHT);
        context.enableScissor(lx + 2, ly + 2, contentRight, ly + LIST_HEIGHT - 2);
        for (int index = firstIndex; index < entries.size(); index++) {
            FrequencyEntry entry = entries.get(index);
            int rowY = ly + 2 + index * ROW_HEIGHT - offset;
            if (rowY >= ly + LIST_HEIGHT - 2) break;
            boolean active = entry.name().equals(selected);
            if (active) context.fill(lx + 2, rowY - 1, contentRight,
                    rowY + ROW_HEIGHT - 1, 0xFF285E60);
            String label = entry.creator().isEmpty() ? entry.name()
                    : entry.name() + " (" + entry.creator() + ")";
            label = fit(label, (int) ((LIST_WIDTH - 15) / LIST_TEXT_SCALE));
            context.getMatrices().push();
            context.getMatrices().translate(lx + 3, rowY + 1, 0);
            context.getMatrices().scale(LIST_TEXT_SCALE, LIST_TEXT_SCALE, 1.0f);
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
        int fx = x + INPUT_X;
        int fy = y + INPUT_Y;
        context.fill(fx, fy, fx + INPUT_WIDTH, fy + INPUT_HEIGHT, 0xFF343434);
        context.fill(fx + 1, fy + 1, fx + INPUT_WIDTH - 1, fy + INPUT_HEIGHT - 1, 0xFF171717);
        if (frequencyField != null && frequencyField.getText().isEmpty())
            context.drawText(textRenderer,
                    Text.translatable("gui.phaseteleporters.frequency_name"),
                    fx + 3, fy + 3, 0xFF777777, false);
        boolean enabled = frequencyField != null
                && !LocalFrequencyState.normalize(frequencyField.getText()).isEmpty()
                && frequencies().stream().noneMatch(entry -> entry.name().equals(
                        LocalFrequencyState.normalize(frequencyField.getText())));
        boolean hovered = inside(mouseX, mouseY, x + CHECK_X, y + CHECK_Y,
                CHECK_SIZE, CHECK_SIZE);
        int checkU = enabled ? (hovered ? 203 : 189) : 217;
        context.drawTexture(BACKGROUND, x + CHECK_X, y + CHECK_Y,
                checkU, 23, CHECK_SIZE, CHECK_SIZE, 256, 256);
        if (!enabled)
            context.fill(x + CHECK_X, y + CHECK_Y,
                    x + CHECK_X + CHECK_SIZE, y + CHECK_Y + CHECK_SIZE,
                    0x66000000);
    }

    private String fit(String value, int width) {
        if (textRenderer.getWidth(value) <= width) return value;
        while (!value.isEmpty() && textRenderer.getWidth(value + "…") > width)
            value = value.substring(0, value.length() - 1);
        return value + "…";
    }

    private int colorButtonX() { return x + backgroundWidth - 3; }
    private int colorButtonY() { return Math.max(y + 30, sidePanel.rightPanelBottom(y)); }
    private int colorPanelX() {
        int right = x + backgroundWidth - 3;
        if (right + PANEL_WIDTH <= width - 2) return right;
        if (x - PANEL_WIDTH + 3 >= 2) return x - PANEL_WIDTH + 3;
        return right;
    }
    private int colorPanelY() { return Math.min(colorButtonY(), height - PANEL_HEIGHT - 2); }

    private void setColorOpen(boolean open) {
        colorOpen = open && showsColorConfiguration();
        updateColorPanelLayout();
    }

    private void updateColorPanelLayout() {
        int centeredX = (width - backgroundWidth) / 2;
        int targetX = centeredX;
        if (colorOpen && centeredX + backgroundWidth + PANEL_WIDTH - 1 > width
                && centeredX - PANEL_WIDTH + 3 < 2)
            targetX = Math.max(2, width - backgroundWidth - PANEL_WIDTH + 1);
        int shift = targetX - x;
        x = targetX;
        if (shift != 0)
            for (var child : children())
                if (child instanceof ClickableWidget widget) widget.setX(widget.getX() + shift);
    }

    private int colorSlotX(int position) {
        int rowStart = position / COLOR_COLUMNS * COLOR_COLUMNS;
        int rowCount = Math.min(COLOR_COLUMNS, PortalColors.count() - rowStart);
        return colorPanelX() + 10 + (COLOR_COLUMNS - rowCount) * COLOR_STEP / 2
                + position % COLOR_COLUMNS * COLOR_STEP;
    }

    private int colorSlotY(int position) {
        return colorPanelY() + 30 + position / COLOR_COLUMNS * COLOR_STEP;
    }

    private void drawColorPanel(DrawContext context, int mouseX, int mouseY) {
        int px = colorPanelX();
        int py = colorPanelY();
        MinecraftClient.getInstance().getTextureManager().getTexture(COLOR_PANEL).setFilter(false, false);
        MinecraftClient.getInstance().getTextureManager().getTexture(COLOR_SLOT).setFilter(false, false);
        context.drawTexture(COLOR_PANEL, px, py, 43, 27, PANEL_WIDTH, PANEL_HEIGHT, 256, 256);
        context.drawText(textRenderer, Text.translatable("gui.phaseteleporters.color_button"),
                px + 12, py + 11, 0x404040, false);
        context.fill(px + 6, py + 26, px + 129, py + 125, 0xFF777777);
        context.fill(px + 7, py + 27, px + 128, py + 124, 0xFFA9A9A9);
        context.fill(px + 7, py + 27, px + 128, py + 28, 0xFF989898);
        context.fill(px + 7, py + 27, px + 8, py + 124, 0xFF989898);
        for (int i = 0; i < PortalColors.count(); i++) {
            int sx = colorSlotX(i);
            int sy = colorSlotY(i);
            int id = PortalColors.displayColor(i);
            drawColorSlot(context, sx, sy, id);
            if (id == color || inside(mouseX, mouseY, sx, sy, COLOR_SLOT_SIZE, COLOR_SLOT_SIZE))
                context.drawBorder(sx, sy, COLOR_SLOT_SIZE, COLOR_SLOT_SIZE, 0xFFFFFFFF);
        }
    }

    private void drawColorSlot(DrawContext context, int sx, int sy, int index) {
        context.drawTexture(COLOR_SLOT, sx, sy, 0, 0, COLOR_SLOT_SIZE, COLOR_SLOT_SIZE, 18, 18);
        int rgb = PortalColors.rgb(index);
        context.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF000000 | rgb);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)) {
                setColorOpen(false);
                return true;
            }
            if (showsColorConfiguration() && !colorOpen && inside(mouseX, mouseY, colorButtonX() + 3, colorButtonY(),
                    BUTTON_WIDTH - 3, BUTTON_HEIGHT)) {
                setColorOpen(true);
                sidePanel.closeAll();
                playClick();
                return true;
            }
            if (colorOpen) {
                int px = colorPanelX();
                int py = colorPanelY();
                if (inside(mouseX, mouseY, px + 3, py, PANEL_WIDTH - 8, 23)) {
                    setColorOpen(false);
                    playClick();
                    return true;
                }
                for (int i = 0; i < PortalColors.count(); i++) {
                    int sx = colorSlotX(i);
                    int sy = colorSlotY(i);
                    if (inside(mouseX, mouseY, sx, sy, COLOR_SLOT_SIZE, COLOR_SLOT_SIZE)) {
                        color = PortalColors.displayColor(i);
                        if (!assigned.isEmpty()) sendColor(assigned, assignedPrivate, color);
                        setColorOpen(false);
                        playClick();
                        return true;
                    }
                }
                if (inside(mouseX, mouseY, px, py, PANEL_WIDTH, PANEL_HEIGHT)) return true;
                setColorOpen(false);
                playClick();
            }
            int lx = x + LIST_X;
            int ly = y + LIST_Y;
            if (inside(mouseX, mouseY, lx + LIST_WIDTH - 8, ly,
                    8, LIST_HEIGHT)) {
                draggingScroll = true;
                scrollTo(mouseY);
                return true;
            }
            if (inside(mouseX, mouseY, lx + 1, ly + 1,
                    LIST_WIDTH - 10, LIST_HEIGHT - 2)) {
                int index = (int) Math.floor((mouseY - ly - 2
                        + Math.round(scrollPixels)) / ROW_HEIGHT);
                if (mouseY >= ly + 2 && mouseY < ly + LIST_HEIGHT - 2
                        && index >= 0 && index < frequencies().size()) {
                    selected = frequencies().get(index).name();
                    if (frequencyField != null) frequencyField.setFocused(false);
                    updateButtons();
                    playClick();
                }
                return true;
            }
            if (inside(mouseX, mouseY, x + CHECK_X, y + CHECK_Y,
                    CHECK_SIZE, CHECK_SIZE)) {
                createFrequency();
                return true;
            }
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
        if (button == 0 && draggingScroll) {
            draggingScroll = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
            double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_WIDTH, LIST_HEIGHT)) {
            scrollPixels = Math.clamp(scrollPixels - verticalAmount * 4.0,
                    0.0, maxScrollPixels());
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

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void playClick() {
        MinecraftClient.getInstance().getSoundManager().play(
                PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        MinecraftClient.getInstance().getTextureManager().getTexture(PUBLIC_SLOT)
                .setFilter(false, false);
        context.drawTexture(PUBLIC_SLOT, x + TAB_X + 5, y + TAB_Y + 5,
                36, 19, 9, 9, 256, 256);
        context.drawTexture(PRIVATE_LOCK, x + TAB_X + 6, y + TAB_Y + TAB_SIZE + TAB_GAP + 5,
                51, 19, 7, 9, 256, 256);
        if (colorOpen) {
            if (inside(mouseX, mouseY, colorPanelX(), colorPanelY(), PANEL_WIDTH, PANEL_HEIGHT)) {
                for (int i = 0; i < PortalColors.count(); i++) {
                    if (inside(mouseX, mouseY, colorSlotX(i), colorSlotY(i), COLOR_SLOT_SIZE, COLOR_SLOT_SIZE)) {
                        int id = PortalColors.displayColor(i);
                        context.drawTooltip(textRenderer, Text.translatable(PortalColors.nameKey(id))
                                .styled(style -> style.withColor(PortalColors.rgb(id))), mouseX, mouseY);
                        break;
                    }
                }
                return;
            }
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
        if (inside(mouseX, mouseY, x + ENERGY_X - 1, y + ENERGY_Y - 1,
                ENERGY_WIDTH + 2, ENERGY_HEIGHT + 2))
            context.drawTooltip(textRenderer,
                    Text.literal(PEGuiText.teleporterEnergy(getEnergy(), getMaxEnergy())),
                    mouseX, mouseY);
        if (showsPortalStatus() && inside(mouseX, mouseY, x + STATUS_X, y + STATUS_Y, STATUS_SIZE, STATUS_SIZE))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.portal_status."
                            + PortalStatus.key(getPortalStatus())), mouseX, mouseY);
        if (inside(mouseX, mouseY, x + TAB_X, y + TAB_Y, TAB_SIZE, TAB_SIZE))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.show_public_frequencies"), mouseX, mouseY);
        else if (inside(mouseX, mouseY, x + TAB_X, y + TAB_Y + TAB_SIZE + TAB_GAP,
                TAB_SIZE, TAB_SIZE))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.show_private_frequencies"), mouseX, mouseY);
        if (showsColorConfiguration() && !colorOpen && inside(mouseX, mouseY, colorButtonX() + 3, colorButtonY(),
                BUTTON_WIDTH - 3, BUTTON_HEIGHT))
            context.drawTooltip(textRenderer,
                    Text.translatable("gui.phaseteleporters.color_button"), mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        Text heading = guiTitle();
        int textWidth = Math.max(1, textRenderer.getWidth(heading));
        float scale = Math.min(1.0f, (float) (LIST_WIDTH - 4) / textWidth);
        context.getMatrices().push();
        context.getMatrices().translate(LIST_X + LIST_WIDTH / 2.0, 5, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(textRenderer, heading, -textWidth / 2, 0, 0xFF404040, false);
        context.getMatrices().pop();
        String owner = assigned.isEmpty() ? "" : (assignedPrivate ? privateFrequencies : publicFrequencies)
                .stream().filter(entry -> entry.name().equals(assigned))
                .map(FrequencyEntry::creator).findFirst().orElse("");
        drawFrequencyInfo(context, 28, "gui.phaseteleporters.info.frequency",
                assigned.isEmpty() ? Text.translatable("gui.phaseteleporters.no_frequency").getString() : assigned,
                assigned.isEmpty() ? 0xFF777777 : 0xFF404040);
        drawFrequencyInfo(context, 40, "gui.phaseteleporters.info.owner",
                owner.isEmpty() ? "—" : owner, owner.isEmpty() ? 0xFF777777 : 0xFF39FF39);
        drawFrequencyInfo(context, 52, "gui.phaseteleporters.info.security",
                assigned.isEmpty() ? "—" : Text.translatable(assignedPrivate
                        ? "gui.phaseteleporters.private" : "gui.phaseteleporters.public").getString(),
                assigned.isEmpty() ? 0xFF777777 : 0xFF39FF39);
    }

    private void drawFrequencyInfo(DrawContext context, int rowY, String key,
            String value, int valueColor) {
        Text label = Text.translatable(key);
        int labelWidth = textRenderer.getWidth(label);
        context.drawText(textRenderer, label, LIST_X, rowY, 0xFF404040, false);
        int valueX = LIST_X + labelWidth + 3;
        int availableWidth = LIST_X + LIST_WIDTH - valueX;
        if (availableWidth > 0)
            context.drawText(textRenderer, fit(value, availableWidth),
                    valueX, rowY, valueColor, false);
    }
}
