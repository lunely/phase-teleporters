package example.phaseteleporters;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;

public final class TeleportationFrameMonitorRenderer implements BlockEntityRenderer<TeleportationFrameMonitorBlockEntity> {
    public TeleportationFrameMonitorRenderer(net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context context) {}
    public static void register() {
        BlockEntityRendererFactories.register(PhaseTeleportersMod.TELEPORTATION_FRAME_MONITOR_BLOCK_ENTITY,
                TeleportationFrameMonitorRenderer::new);
    }

    @Override public void render(TeleportationFrameMonitorBlockEntity monitor, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        if (monitor.text().isBlank() || monitor.getWorld() == null) return;
        Direction facing = monitor.getCachedState().get(TeleportationFrameMonitorBlock.FACING);
        float yaw = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 0;
            case WEST -> -90;
            default -> 180;
        };
        var renderer = MinecraftClient.getInstance().textRenderer;
        var lines = monitor.text().split("\\n", -1);
        int visible = Math.min(4, lines.length);
        while (visible > 1 && lines[visible-1].isBlank()) visible--;
        int widest = 1;
        for (int i = 0; i < visible; i++) widest = Math.max(widest, renderer.getWidth(lines[i]));
        float scale = Math.min(0.0105f, Math.min(0.875f / widest, 0.625f / (visible * 10)));
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        // Font quads face local +Z; point that side outward, in front of the display.
        matrices.translate(0, 0, 0.5 + 0.501 / 16.0);
        matrices.scale(scale, -scale, scale);
        for (int i = 0; i < visible; i++) {
            var line = Text.literal(lines[i]);
            float x = -renderer.getWidth(line) / 2.0f;
            float y = i * 10 - visible * 5;
            renderer.draw(line, x, y, 0xFF000000 | PortalColors.rgb(monitor.textColor()), false,
                    matrices.peek().getPositionMatrix(), consumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.POLYGON_OFFSET,
                    0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        matrices.pop();
    }
}
