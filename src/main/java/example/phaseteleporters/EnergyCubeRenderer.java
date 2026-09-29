package example.phaseteleporters;

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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Client-only visual shared by the placed cubes and their item renderers. */
public final class EnergyCubeRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final Identifier CORE = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_core.png");
    private static final Identifier SHELL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_core_shell.png");
    private static final Identifier GLASS = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "textures/block/energy_cube_glass.png");
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
        renderCore(time, matrices, consumers, overlay);
        renderGlass(matrices, consumers, light, overlay);
    }

    private void renderGlass(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        // Stationary glazing sits inside the existing frame, behind the six ports.
        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.scale(1.625f, 1.625f, 1.625f);
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
