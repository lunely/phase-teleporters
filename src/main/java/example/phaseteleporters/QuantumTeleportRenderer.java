package example.phaseteleporters;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

/** A black spherical core and matching amber port outlines. */
public final class QuantumTeleportRenderer implements BlockEntityRenderer<QuantumTeleportBlockEntity> {
    private static final Identifier MATERIAL = Identifier.ofVanilla("textures/block/white_concrete.png");
    private static final int GLOW_COLOR = 0xFFF0A366;
    private static final Identifier GLASS = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_glass.png");
    private static final ModelPart GLASS_MODEL = createGlass();
    private static final int SLICES = 32;
    private static final int BANDS = 20;

    public static void register() {
        BlockEntityRendererFactories.register(PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY,
                context -> new QuantumTeleportRenderer());
        var state = PhaseTeleportersMod.QUANTUM_TELEPORT.getDefaultState();
        BuiltinItemRendererRegistry.INSTANCE.register(PhaseTeleportersMod.QUANTUM_TELEPORT_ITEM,
                (stack, mode, matrices, consumers, light, overlay) -> {
                    MinecraftClient.getInstance().getBlockRenderManager()
                            .renderBlockAsEntity(state, matrices, consumers, light, overlay);
                    renderCore(matrices, consumers, overlay);
                    renderPortOutlines(matrices, consumers, light, overlay);
                    renderFrontMarker(matrices, consumers, light, overlay, Direction.NORTH);
                    renderGlass(matrices, consumers, light, overlay);
                });
    }

    @Override public void render(QuantumTeleportBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay) {
        renderCore(matrices, consumers, overlay);
        renderPortOutlines(matrices, consumers, light, overlay);
        renderFrontMarker(matrices, consumers, light, overlay,
                entity.getCachedState().get(QuantumTeleportBlock.FACING));
        renderGlass(matrices, consumers, light, overlay);
    }

    private static ModelPart createGlass() {
        ModelData data = new ModelData();
        data.getRoot().addChild("glass", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -4, -4, 8, 8, 8), ModelTransform.NONE);
        return TexturedModelData.of(data, 32, 32).createModel();
    }

    private static void renderGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        // Keep the dark glass just inside the metal frame. At 1.625 its faces
        // sit on the same planes as the frame and can flicker while moving.
        matrices.scale(1.60f, 1.60f, 1.60f);
        GLASS_MODEL.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(GLASS)),
                light, overlay, 0xFF121115);
        matrices.pop();
    }

    private static void renderPortOutlines(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(MATERIAL));
        for (int axis = 0; axis < 3; axis++) {
            for (int side = -1; side <= 1; side += 2) {
                float plane = (side < 0 ? -0.025f : 16.025f) / 16.0f;
                // Match the cube's 32 px port artwork: gap 6..7, accent 7..9,
                // recessed inner rim 9..11, then the black center. Each port is 5 model pixels wide.
                outline(matrices.peek(), vertices, axis, side, plane, portPixel(6), portPixel(26), portWidth(1),
                        0xFF15191D, light, overlay);
                outline(matrices.peek(), vertices, axis, side, plane, portPixel(7), portPixel(25), portWidth(2),
                        GLOW_COLOR, light, overlay);
                outline(matrices.peek(), vertices, axis, side, plane, portPixel(9), portPixel(23), portWidth(2),
                        0xFF20252A, light, overlay);
            }
        }
    }

    private static float portPixel(int pixel) { return 5.5f + portWidth(pixel); }
    private static void renderFrontMarker(MatrixStack matrices, VertexConsumerProvider consumers,
            int light, int overlay, Direction front) {
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(MATERIAL));
        boolean xAxis = front.getAxis() == Direction.Axis.X;
        int side = front.getDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        float plane = side < 0 ? -0.025f / 16 : 16.025f / 16;
        // Small amber dash above the front port identifies the face used by the GUI.
        portQuad(matrices.peek(), vertices, xAxis ? 0 : 2, side, plane,
                (xAxis ? 11 : 6.5f) / 16, (xAxis ? 6.5f : 11) / 16,
                (xAxis ? 12 : 9.5f) / 16, (xAxis ? 9.5f : 12) / 16,
                GLOW_COLOR, light, overlay);
    }
    private static float portWidth(int pixels) { return pixels * 5.0f / 32.0f; }

    private static void outline(MatrixStack.Entry matrix, VertexConsumer vertices, int axis, int side,
            float plane, float min, float max, float thickness, int color, int light, int overlay) {
        portQuad(matrix, vertices, axis, side, plane, min, min, max, min + thickness, color, light, overlay);
        portQuad(matrix, vertices, axis, side, plane, min, max - thickness, max, max, color, light, overlay);
        portQuad(matrix, vertices, axis, side, plane, min, min + thickness, min + thickness, max - thickness, color, light, overlay);
        portQuad(matrix, vertices, axis, side, plane, max - thickness, min + thickness, max, max - thickness, color, light, overlay);
    }

    private static void portQuad(MatrixStack.Entry matrix, VertexConsumer vertices, int axis, int side,
            float plane, float a0, float b0, float a1, float b1, int color, int light, int overlay) {
        float[][] corners = {{a0, b0}, {a1, b0}, {a1, b1}, {a0, b1}};
        // XY and YZ wind toward the positive axis; XZ winds toward negative Y.
        boolean reverse = side != (axis == 1 ? -1 : 1);
        for (int i = 0; i < 4; i++) {
            float[] corner = corners[reverse ? 3 - i : i];
            float a = corner[0] / 16, b = corner[1] / 16;
            float x = axis == 0 ? plane : a;
            float y = axis == 0 ? a : axis == 1 ? plane : b;
            float z = axis == 2 ? plane : b;
            vertices.vertex(matrix, x, y, z).color(color).texture(0.5f, 0.5f)
                    .overlay(overlay).light(light)
                    .normal(matrix, axis == 0 ? side : 0, axis == 1 ? side : 0, axis == 2 ? side : 0);
        }
    }

    private static void renderCore(MatrixStack matrices, VertexConsumerProvider consumers, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(MATERIAL));
        sphere(matrices.peek(), vertices, 0.2625f, GLOW_COLOR, overlay, true);
        sphere(matrices.peek(), vertices, 0.25f, 0xFF020103, overlay, false);
        matrices.pop();
    }

    private static void sphere(MatrixStack.Entry matrix, VertexConsumer vertices,
            float radius, int color, int overlay, boolean inside) {
        for (int band = 0; band < BANDS; band++) {
            double a = Math.PI * band / BANDS;
            double b = Math.PI * (band + 1) / BANDS;
            for (int slice = 0; slice < SLICES; slice++) {
                double c = 2 * Math.PI * slice / SLICES;
                double d = 2 * Math.PI * (slice + 1) / SLICES;
                if (inside) {
                    vertex(matrix, vertices, radius, a, c, color, overlay, -1);
                    vertex(matrix, vertices, radius, b, c, color, overlay, -1);
                    vertex(matrix, vertices, radius, b, d, color, overlay, -1);
                    vertex(matrix, vertices, radius, a, d, color, overlay, -1);
                } else {
                    vertex(matrix, vertices, radius, a, d, color, overlay, 1);
                    vertex(matrix, vertices, radius, b, d, color, overlay, 1);
                    vertex(matrix, vertices, radius, b, c, color, overlay, 1);
                    vertex(matrix, vertices, radius, a, c, color, overlay, 1);
                }
            }
        }
    }

    private static void vertex(MatrixStack.Entry matrix, VertexConsumer vertices, float radius,
            double latitude, double longitude, int color, int overlay, int normalSign) {
        float x = (float) (Math.sin(latitude) * Math.cos(longitude));
        float y = (float) Math.cos(latitude);
        float z = (float) (Math.sin(latitude) * Math.sin(longitude));
        vertices.vertex(matrix, x * radius, y * radius, z * radius).color(color)
                .texture(0.5f, 0.5f).overlay(overlay).light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                .normal(matrix, x * normalSign, y * normalSign, z * normalSign);
    }
}
