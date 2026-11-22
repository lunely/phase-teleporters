package example.phaseteleporters;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

public final class InfusionStationRenderer implements BlockEntityRenderer<InfusionStationBlockEntity> {
    private static final RenderLayer REDSTONE = resourceLayer("redstone");
    private static final RenderLayer DIAMOND = resourceLayer("diamond");
    private static final RenderLayer GLOWSTONE = resourceLayer("glowstone");
    private static final RenderLayer OBSIDIAN = resourceLayer("refined_obsidian");
    private static final RenderLayer CARBON = resourceLayer("carbon");

    public InfusionStationRenderer(BlockEntityRendererFactory.Context context) {}

    static void register() {
        BlockEntityRendererFactories.register(PhaseTeleportersMod.INFUSION_STATION_BLOCK_ENTITY,
                InfusionStationRenderer::new);
    }

    private static RenderLayer resourceLayer(String name) {
        return RenderLayer.getEntityTranslucent(Identifier.of(
                PhaseTeleportersMod.MOD_ID, "textures/block/infusion_resource/" + name + ".png"));
    }

    @Override
    public void render(InfusionStationBlockEntity station, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay) {
        int rows = station.displayLevel();
        if (rows == 0) return;
        RenderLayer layer = switch (station.displayResource()) {
            case REDSTONE -> REDSTONE;
            case DIAMOND -> DIAMOND;
            case GLOWSTONE_DUST -> GLOWSTONE;
            case PURIFIED_OBSIDIAN_DUST -> OBSIDIAN;
            case COAL -> CARBON;
            case NONE -> null;
        };
        if (layer == null) return;

        Direction facing = station.getCachedState().get(InfusionStationBlock.FACING);
        if (station.getWorld() != null) {
            light = WorldRenderer.getLightmapCoordinates(station.getWorld(), station.getPos().offset(facing));
        }
        float yaw = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 0;
            case WEST -> -90;
            default -> 180;
        };
        float fill = rows / (float) InfusionStationBlockEntity.DISPLAY_ROWS;
        float top = 0.5f * fill - 0.25f;
        float left = -0.125f;
        float right = 0.125f;
        float bottom = -0.25f;

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        MatrixStack.Entry entry = matrices.peek();
        VertexConsumer vertices = consumers.getBuffer(layer);
        vertex(vertices, entry, left, bottom, 0, 1, light, overlay);
        vertex(vertices, entry, right, bottom, 1, 1, light, overlay);
        vertex(vertices, entry, right, top, 1, 1 - fill, light, overlay);
        vertex(vertices, entry, left, top, 0, 1 - fill, light, overlay);
        matrices.pop();
    }

    private static void vertex(VertexConsumer vertices, MatrixStack.Entry entry,
            float x, float y, float u, float v, int light, int overlay) {
        vertices.vertex(entry, x, y, 0.501f).color(0xFFFFFFFF).texture(u, v)
                .overlay(overlay).light(light).normal(entry, 0, 0, 1);
    }
}
