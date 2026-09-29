package example.phaseteleporters;

import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ItemDescriptionTooltip {
    private static final int MAX_LINE_WIDTH = 220;
    private static KeyBinding detailsKey;

    private ItemDescriptionTooltip() {}

    public static void register() {
        detailsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.phaseteleporters.item_details", InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_SHIFT, "key.categories.phaseteleporters"));
        ItemTooltipCallback.EVENT.register(ItemDescriptionTooltip::append);
    }

    private static void append(ItemStack stack, Item.TooltipContext context,
            TooltipType type, List<Text> tooltip) {
        Identifier id = Registries.ITEM.getId(stack.getItem());
        if (!PhaseTeleportersMod.MOD_ID.equals(id.getNamespace())) return;
        String key = "tooltip.phaseteleporters.description." + id.getPath();
        if (!I18n.hasTranslation(key)) return;
        String description = I18n.translate(key);
        if (description.isBlank()) return;

        if (isDetailsHeld()) {
            addWrappedDescription(description, tooltip);
        } else {
            tooltip.add(Text.translatable("tooltip.phaseteleporters.item_details_hint",
                    detailsKey.getBoundKeyLocalizedText()).formatted(Formatting.DARK_GRAY));
        }
    }

    private static boolean isDetailsHeld() {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(detailsKey);
        if (key.getCode() < 0) return false;
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE)
            return GLFW.glfwGetMouseButton(window, key.getCode()) == GLFW.GLFW_PRESS;
        if (key.getCategory() == InputUtil.Type.KEYSYM)
            return InputUtil.isKeyPressed(window, key.getCode());
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++)
            if (GLFW.glfwGetKeyScancode(code) == key.getCode()
                    && InputUtil.isKeyPressed(window, code)) return true;
        return false;
    }

    private static void addWrappedDescription(String description, List<Text> tooltip) {
        var renderer = MinecraftClient.getInstance().textRenderer;
        for (String paragraph : description.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                tooltip.add(Text.empty());
                continue;
            }
            String line = "";
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && renderer.getWidth(candidate) > MAX_LINE_WIDTH) {
                    tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
                    line = word;
                } else {
                    line = candidate;
                }
            }
            if (!line.isEmpty()) tooltip.add(Text.literal(line).formatted(Formatting.GRAY));
        }
    }
}
