package example.phaseteleporters;

import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
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

    private ItemDescriptionTooltip() {}

    public static void register() {
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
                    "LSHIFT")
                    .formatted(Formatting.DARK_GRAY));
        }
    }

    private static boolean isDetailsHeld() {
        MinecraftClient client = MinecraftClient.getInstance();
        long window = client.getWindow().getHandle();
        return InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private static void addWrappedDescription(String description, List<Text> tooltip) {
        var renderer = MinecraftClient.getInstance().textRenderer;
        for (String paragraph : description.split("\\R", -1)) {
            if (paragraph.isBlank()) {
                tooltip.add(Text.empty());
                continue;
            }
            for (var line : renderer.getTextHandler().wrapLines(
                    paragraph, MAX_LINE_WIDTH, net.minecraft.text.Style.EMPTY)) {
                tooltip.add(Text.literal(line.getString()).formatted(Formatting.GRAY));
            }
        }
    }
}
