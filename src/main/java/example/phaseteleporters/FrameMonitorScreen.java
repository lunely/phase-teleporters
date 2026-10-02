package example.phaseteleporters;

import java.util.Arrays;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class FrameMonitorScreen extends Screen {
    private final BlockPos pos;
    private final String[] lines = new String[4];
    private SelectionManager selection;
    private int currentLine;
    private int ticks;
    private boolean sent;

    public FrameMonitorScreen(BlockPos pos, String text) {
        super(Text.translatable("gui.phaseteleporters.frame_monitor.title"));
        this.pos = pos;
        Arrays.fill(lines, "");
        String[] initial = text.split("\\n", -1);
        System.arraycopy(initial, 0, lines, 0, Math.min(initial.length, lines.length));
    }

    @Override protected void init() {
        selection = new SelectionManager(() -> lines[currentLine], value -> lines[currentLine] = value,
                SelectionManager.makeClipboardGetter(client), SelectionManager.makeClipboardSetter(client),
                value -> value.length() <= TeleportationFrameMonitorBlockEntity.MAX_LINE_LENGTH
                        && textRenderer.getWidth(value) <= 90 && value.indexOf('\n') < 0);
        selection.putCursorAtEnd();
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
                .dimensions(width / 2 - 100, height / 2 + 65, 200, 20).build());
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void tick() { ticks++; }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            currentLine = Math.floorMod(currentLine + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1), 4);
            selection.putCursorAtEnd();
            return true;
        }
        if (selection.handleSpecialKey(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public boolean charTyped(char chr, int modifiers) {
        return selection.insert(chr) || super.charTyped(chr, modifiers);
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int top = height / 2 - 40;
        if (button == 0 && mouseX >= width / 2 - 100 && mouseX <= width / 2 + 100
                && mouseY >= top && mouseY < top + 80) {
            currentLine = (int) ((mouseY - top) / 20);
            selection.putCursorAtEnd();
            return true;
        }
        return false;
    }

    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x60000000);
    }

    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 85, 0xFFFFFFFF);
        context.getMatrices().push();
        context.getMatrices().translate(width / 2, height / 2 - 40, 0);
        context.getMatrices().scale(2, 2, 2);
        for (int i = 0; i < 4; i++) {
            String line = lines[i];
            int x = -textRenderer.getWidth(line) / 2;
            int y = i * 10;
            if (i == currentLine) {
                int start = Math.min(selection.getSelectionStart(), line.length());
                int end = Math.min(selection.getSelectionEnd(), line.length());
                int cursorX = x + textRenderer.getWidth(line.substring(0, start));
                if (start != end) {
                    int endX = x + textRenderer.getWidth(line.substring(0, end));
                    context.fill(Math.min(cursorX, endX), y, Math.max(cursorX, endX), y + 9, 0xAA3366AA);
                } else if ((ticks / 6) % 2 == 0) {
                    context.fill(cursorX, y, cursorX + 1, y + 9, 0xFFFFFFFF);
                }
            }
            context.drawText(textRenderer, line, x, y, 0xFFFFFFFF, false);
        }
        context.getMatrices().pop();
    }

    @Override public void close() {
        if (!sent) {
            ClientPlayNetworking.send(new FrameMonitorTextPayload(pos, String.join("\n", lines)));
            sent = true;
        }
        super.close();
    }
}
