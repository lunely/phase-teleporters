package example.phaseteleporters;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
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
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/** Renders the spherical core and glass inside the textured block model. */
public final class QuantumTeleportRenderer implements BlockEntityRenderer<QuantumTeleportBlockEntity> {
    private static final Identifier MATERIAL = Identifier.ofVanilla("textures/block/white_concrete.png");
    private static final int CORE_COLOR = 0xFFF0A366;
    private static final Identifier GLASS = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_glass.png");
    private static final ModelPart GLASS_MODEL = createGlass();
    private static final Identifier GLASS_PANES_MODEL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/quantum_teleporter_glass");
    private static final Random GLASS_RANDOM = Random.create(0);
    private static final int SLICES = 32;
    private static final int BANDS = 20;

    public static void register() {
        QuantumPortModel.register();
        ModelLoadingPlugin.register(context -> context.addModels(GLASS_PANES_MODEL));
        BlockEntityRendererFactories.register(PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY,
                context -> new QuantumTeleportRenderer());
        var state = PhaseTeleportersMod.QUANTUM_TELEPORT.getDefaultState();
        BuiltinItemRendererRegistry.INSTANCE.register(PhaseTeleportersMod.QUANTUM_TELEPORT_ITEM,
                (stack, mode, matrices, consumers, light, overlay) -> {
                    MinecraftClient.getInstance().getBlockRenderManager()
                            .renderBlockAsEntity(state, matrices, consumers, light, overlay);
                    QuantumPortModel.renderLeds(matrices, consumers);
                    renderCore(matrices, consumers, overlay);
                    renderGlass(matrices, consumers, light, overlay);
                });
    }

    @Override public void render(QuantumTeleportBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay) {
        QuantumPortModel.renderLeds(matrices, consumers);
        renderCore(matrices, consumers, overlay);
        renderGlass(matrices, consumers, light, overlay);
    }

    private static ModelPart createGlass() {
        ModelData data = new ModelData();
        data.getRoot().addChild("glass", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4, -4, -4, 8, 8, 8), ModelTransform.NONE);
        ModelPart model = TexturedModelData.of(data, 32, 32).createModel();
        // Keep the glass geometry available, but hide it in both render paths.
        model.getChild("glass").visible = false;
        return model;
    }

    private static void renderGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        // Fetch on each render so fitted pane geometry follows resource reloads.
        var panes = MinecraftClient.getInstance().getBakedModelManager().getModel(GLASS_PANES_MODEL);
        var vertices = consumers.getBuffer(
                RenderLayer.getEntityTranslucent(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        GLASS_RANDOM.setSeed(0);
        for (var quad : panes.getQuads(null, null, GLASS_RANDOM)) {
            vertices.quad(matrices.peek(), quad, 8 / 255.0f, 8 / 255.0f, 10 / 255.0f, 1.0f,
                    light, overlay);
        }
    }

    private static void renderLegacyGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        // Enclose the central core rather than covering the outer frame.
        // The recessed structural rails stay outside this inner enclosure.
        matrices.scale(1.20f, 1.20f, 1.20f);
        GLASS_MODEL.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(GLASS)),
                light, overlay, 0xFF121115);
        matrices.pop();
    }

    private static void renderCore(MatrixStack matrices, VertexConsumerProvider consumers, int overlay) {
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntitySolid(MATERIAL));
        sphere(matrices.peek(), vertices, 0.2625f, CORE_COLOR, overlay, true);
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
