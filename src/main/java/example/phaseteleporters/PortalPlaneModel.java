package example.phaseteleporters;

import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/** Keeps portal colors bright without adding light to the surrounding world. */
public final class PortalPlaneModel extends ForwardingBakedModel {
    private static final Set<String> MODELS = Set.of(
            "block/portal_plane_x", "block/portal_plane_z",
            "block/interdimensional_portal_plane_x", "block/interdimensional_portal_plane_z");
    private final RenderMaterial material;

    private PortalPlaneModel(BakedModel wrapped, RenderMaterial material) {
        super(wrapped);
        this.material = material;
    }

    public static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            var id = context.resourceId();
            if (model == null || id == null || !id.getNamespace().equals(PhaseTeleportersMod.MOD_ID)
                    || !MODELS.contains(id.getPath())) return model;
            var renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer == null) return model;
            var material = renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT)
                    .emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE).find();
            return new PortalPlaneModel(model, material);
        }));
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView world, BlockState state, BlockPos pos,
            Supplier<Random> randomSupplier, RenderContext context) {
        context.pushTransform(quad -> {
            quad.material(material);
            return true;
        });
        try {
            super.emitBlockQuads(world, state, pos, randomSupplier, context);
        } finally {
            context.popTransform();
        }
    }
}
