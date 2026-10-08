package example.phaseteleporters;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;

/** Client-only visual shared by the placed cubes and their item renderers. */
public final class EnergyCubeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final Identifier CORE = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_core.png");
    private static final Identifier SHELL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_core_shell.png");
    private static final Identifier GLASS = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_glass.png");
    private static final Identifier GLASS_PANES_MODEL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/quantum_teleporter_glass");
    private static final Random GLASS_RANDOM = Random.create(0);
    private static final Identifier ENERGY_LED_MODEL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/energy_cube_port_led");
    private static final Identifier CREATIVE_LED_MODEL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/creative_energy_cube_port_led");
    private static final Random LED_RANDOM = Random.create(0);
    private final ModelPart core = cube(6);
    private final ModelPart shell = cube(7);
    private final ModelPart glass = cube(8);
    private final boolean creative;

    private EnergyCubeRenderer(boolean creative) {
        this.creative = creative;
    }

    private static ModelPart cube(int size) {
        ModelData data = new ModelData();
        float half = size / 2.0f;
        data.getRoot().addChild("core", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-half, -half, -half, size, size, size), ModelTransform.NONE);
        return TexturedModelData.of(data, 32, 32).createModel();
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            context.addModels(ENERGY_LED_MODEL);
            context.addModels(CREATIVE_LED_MODEL);
            context.addModels(GLASS_PANES_MODEL);
        });
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.ENERGY_CUBE, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.CREATIVE_ENERGY_CUBE, RenderLayer.getCutout());
        BlockEntityRendererFactories.register(PhaseTeleportersMod.ENERGY_CUBE_BLOCK_ENTITY,
                context -> new EnergyCubeRenderer<>(false));
        BlockEntityRendererFactories.register(PhaseTeleportersMod.CREATIVE_ENERGY_CUBE_BLOCK_ENTITY,
                context -> new EnergyCubeRenderer<>(true));
        registerItem(false);
        registerItem(true);
    }

    private static void registerItem(boolean creative) {
        EnergyCubeRenderer<BlockEntity> renderer = new EnergyCubeRenderer<>(creative);
        BlockState state = (creative ? PhaseTeleportersMod.CREATIVE_ENERGY_CUBE
                : PhaseTeleportersMod.ENERGY_CUBE).getDefaultState();
        BuiltinItemRendererRegistry.INSTANCE.register(state.getBlock().asItem(),
                (stack, mode, matrices, consumers, light, overlay) -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    // Render the static block model directly, avoiding recursive item rendering.
                    client.getBlockRenderManager().renderBlockAsEntity(state, matrices, consumers, light, overlay);
                    renderer.renderLeds(matrices, consumers);
                    float time = client.world == null ? 0
                            : (client.world.getTime() % 24000) + client.getRenderTickCounter().getTickDelta(false);
                    renderer.renderCore(time, matrices, consumers, overlay);
                    renderer.renderGlass(matrices, consumers, light, overlay);
                });
    }

    @Override
    public void render(T entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider consumers, int light, int overlay) {
        float time = entity.getWorld() == null ? 0
                : (entity.getWorld().getTime() % 24000) + tickDelta;
        renderLeds(matrices, consumers);
        renderCore(time, matrices, consumers, overlay);
        renderGlass(matrices, consumers, light, overlay);
    }

    private void renderLeds(MatrixStack matrices, VertexConsumerProvider consumers) {
        // Reloadable JSON geometry and PNG colors use the same fullbright pass as quantum ports.
        var model = MinecraftClient.getInstance().getBakedModelManager()
                .getModel(creative ? CREATIVE_LED_MODEL : ENERGY_LED_MODEL);
        var vertices = consumers.getBuffer(
                RenderLayer.getEntityCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        LED_RANDOM.setSeed(0);
        for (var quad : model.getQuads(null, null, LED_RANDOM)) {
            vertices.quad(matrices.peek(), quad, 1.0f, 1.0f, 1.0f, 1.0f,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }
    }

    private void renderGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        // Fit the panes to the shared housing and tint them to match the cube's core.
        var panes = MinecraftClient.getInstance().getBakedModelManager().getModel(GLASS_PANES_MODEL);
        var vertices = consumers.getBuffer(
                RenderLayer.getEntityTranslucent(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        int rgb = creative ? 0x6DDFEA : 0x7BDE8A;
        GLASS_RANDOM.setSeed(0);
        for (var quad : panes.getQuads(null, null, GLASS_RANDOM)) {
            vertices.quad(matrices.peek(), quad, ((rgb >> 16) & 255) / 255.0f,
                    ((rgb >> 8) & 255) / 255.0f, (rgb & 255) / 255.0f, 1.0f,
                    light, overlay);
        }
    }

    private void renderLegacyGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        // Stationary glazing sits inside the existing frame, behind the six ports.
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        // Leave a little depth between the glass and the inner frame faces.
        matrices.scale(1.60f, 1.60f, 1.60f);
        glass.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(GLASS)),
                light, overlay, creative ? 0xFF6DDFEA : 0xFF7BDE8A);
        matrices.pop();
    }

    private void renderCore(float time, MatrixStack matrices, VertexConsumerProvider consumers, int overlay) {
        int rgb = creative ? 0x6DDFEA : 0x7BDE8A;
        int fullBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        matrices.push();
        matrices.translate(0.5, 0.5 + Math.sin(time * Math.PI / 100.0) * 0.025, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 0.75f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
        core.render(matrices, consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(CORE)),
                fullBright, overlay, 0xFF000000 | rgb);
        matrices.pop();

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-time * 0.45f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
        shell.render(matrices, consumers.getBuffer(RenderLayer.getEntityTranslucent(SHELL)),
                fullBright, overlay, 0xB0000000 | rgb);
        matrices.pop();
    }
}
