package example.phaseteleporters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SolarPanelScreen extends HandledScreen<SolarPanelScreenHandler> {
    private final EnergySidePanel sidePanel = new EnergySidePanel();
    private static final Identifier BACKGROUND = texture("solar_panel");
    private static final Identifier EMPTY = texture("electric_furnace_energy_empty");
    private static final Identifier FILLED = texture("electric_furnace_energy_filled");
    private static final Identifier ENERGY_SLOT = texture("slot/energy_output");
    private static final int ENERGY_X = 8, ENERGY_Y = 8, ENERGY_WIDTH = 16, ENERGY_HEIGHT = 48;
    private static Identifier texture(String name) { return Identifier.of(PhaseTeleportersMod.MOD_ID, "textures/gui/" + name + ".png"); }
    public SolarPanelScreen(SolarPanelScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 176; backgroundHeight = 166; playerInventoryTitleY = 72;
    }
    @Override protected void init() {
        super.init();
        titleX = backgroundWidth - 8 - textRenderer.getWidth(title);
        playerInventoryTitleX = backgroundWidth - 8 - textRenderer.getWidth(Text.translatable("container.inventory"));
    }
    @Override protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        sidePanel.drawBehind(context, x, y, backgroundWidth, mouseX, mouseY, handler);
        context.drawTexture(BACKGROUND, x, y, 0, 0, 176, 166, 256, 256);
        int left = x + ENERGY_X, top = y + ENERGY_Y, bottom = top + ENERGY_HEIGHT;
        context.fill(left - 1, top - 1, left + ENERGY_WIDTH + 1, bottom + 1, 0xFF373737);
        context.drawTexture(EMPTY, left, top, 0, 0, ENERGY_WIDTH, ENERGY_HEIGHT, ENERGY_WIDTH, ENERGY_HEIGHT);
        int filled = handler.capacity() <= 0 ? 0 : (int) Math.clamp(handler.energy() * ENERGY_HEIGHT / handler.capacity(), 0, ENERGY_HEIGHT);
        if (filled > 0) context.drawTexture(FILLED, left, bottom - filled, 0, ENERGY_HEIGHT - filled, ENERGY_WIDTH, filled, ENERGY_WIDTH, ENERGY_HEIGHT);
        context.drawTexture(ENERGY_SLOT, x + 7, y + 61, 0, 0, 18, 18, 18, 18);
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        if (mouseX >= x + ENERGY_X && mouseX < x + ENERGY_X + ENERGY_WIDTH
                && mouseY >= y + ENERGY_Y && mouseY < y + ENERGY_Y + ENERGY_HEIGHT) {
            context.drawTooltip(textRenderer, java.util.List.of(
                    Text.literal(handler.generation() + " J/t").formatted(net.minecraft.util.Formatting.GREEN),
                    Text.literal(PEGuiText.energy(handler.energy(), handler.capacity()))), mouseX, mouseY);
        }
        sidePanel.tooltip(context, textRenderer, x, y, backgroundWidth, mouseX, mouseY, handler);
    }
    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return sidePanel.click(mouseX, mouseY, button, x, y, backgroundWidth, handler)
                || super.mouseClicked(mouseX, mouseY, button);
    }
}
