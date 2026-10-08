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
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/** Selects authored frame/controller atlas rows and makes only LED faces emissive. */
public final class TeleportationFrameModel extends ForwardingBakedModel {
    private static final Set<String> MODELS = Set.of(
            "block/teleportation_frame", "block/interdimensional_teleportation_frame",
            "item/teleportation_frame", "item/interdimensional_teleportation_frame",
            "block/local_teleporter", "item/local_teleporter",
            "block/interdimensional_teleporter", "item/interdimensional_teleporter",
            "block/emergency_teleporter", "item/emergency_teleporter",
            "block/teleportation_frame_monitor", "item/teleportation_frame_monitor",
            "block/emergency_teleporter_port_led");
    private static final Identifier LED_TEXTURE = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/models/teleporter_frame_led");
    private static final int LED_FACE_MARKER = 1;
    private static final int TOP_LED_FACE_MARKER = 2;
    private static final int FIXED_PORT_LED_FACE_MARKER = 3;
    private static final int MONITOR_LED_FACE_MARKER = 4;
    private final Sprite ledSprite;
    private final Sprite topLedSprite;
    private final RenderMaterial ledMaterial;
    private final RenderMaterial topLedMaterial;
    private final int defaultColor;

    private TeleportationFrameModel(BakedModel wrapped, Sprite ledSprite, Sprite topLedSprite,
            RenderMaterial material, RenderMaterial topMaterial, int defaultColor) {
        super(wrapped);
        this.ledSprite = ledSprite;
        this.topLedSprite = topLedSprite;
        this.ledMaterial = material;
        this.topLedMaterial = topMaterial;
        this.defaultColor = defaultColor;
    }

    public static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyModelAfterBake().register((model, context) -> {
            var id = context.resourceId();
            if (model == null || id == null || !id.getNamespace().equals(PhaseTeleportersMod.MOD_ID)
                    || !MODELS.contains(id.getPath())) return model;
            var renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer == null) return model;
            var material = renderer.materialFinder().blendMode(BlendMode.CUTOUT)
                    .emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE).find();
            boolean local = id.getPath().endsWith("/local_teleporter");
            boolean interdimensional = id.getPath().endsWith("/interdimensional_teleporter");
            boolean emergency = id.getPath().endsWith("/emergency_teleporter");
            String prefix = interdimensional ? "interdimensional_teleporter" : "local_teleporter";
            var sideTexture = local || interdimensional || emergency ? Identifier.of(PhaseTeleportersMod.MOD_ID,
                    "block/models/" + prefix + "_led") : LED_TEXTURE;
            var topTexture = local || interdimensional || emergency ? Identifier.of(PhaseTeleportersMod.MOD_ID,
                    "block/models/" + prefix + "_top_led") : LED_TEXTURE;
            var sprite = context.textureGetter().apply(
                    new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, sideTexture));
            var topSprite = context.textureGetter().apply(
                    new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, topTexture));
            // The interdimensional top has a partially transparent center; cutout would discard it.
            var topMaterial = interdimensional
                    ? renderer.materialFinder().blendMode(BlendMode.TRANSLUCENT)
                            .emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE).find()
                    : material;
            int defaultColor = interdimensional ? InterdimensionalTeleportBlock.DEFAULT_COLOR : FrameColors.DEFAULT;
            return new TeleportationFrameModel(model, sprite, topSprite, material, topMaterial, defaultColor);
        }));
    }

    @Override public boolean isVanillaAdapter() { return false; }

    private void pushLedTransform(RenderContext context, int color, int monitorColor) {
        context.pushTransform(quad -> {
            int marker = quad.colorIndex();
            if (marker == FIXED_PORT_LED_FACE_MARKER) {
                quad.colorIndex(-1);
                quad.material(ledMaterial);
                return true;
            }
            if (marker == MONITOR_LED_FACE_MARKER && monitorColor == TeleportationFrameMonitorBlock.LIGHTS_OFF) return false;
            if (marker != LED_FACE_MARKER && marker != TOP_LED_FACE_MARKER && marker != MONITOR_LED_FACE_MARKER) return true;
            Sprite sprite = marker == TOP_LED_FACE_MARKER ? topLedSprite : ledSprite;
            int selected = marker == MONITOR_LED_FACE_MARKER ? monitorColor : color;
            float offset = (FrameColors.atlasRow(selected) - FrameColors.atlasRow(defaultColor))
                    * (sprite.getMaxV() - sprite.getMinV()) / FrameColors.count();
            quad.colorIndex(-1);
            quad.material(marker == TOP_LED_FACE_MARKER ? topLedMaterial : ledMaterial);
            for (int vertex = 0; vertex < 4; vertex++) {
                quad.uv(vertex, quad.u(vertex), quad.v(vertex) + offset);
            }
            return true;
        });
    }

    @Override public void emitBlockQuads(BlockRenderView world, BlockState state, BlockPos pos,
            Supplier<Random> randomSupplier, RenderContext context) {
        pushLedTransform(context, state.get(TeleportationFrameBlock.COLOR),
                state.contains(TeleportationFrameMonitorBlock.MONITOR_COLOR)
                        ? state.get(TeleportationFrameMonitorBlock.MONITOR_COLOR) : FrameColors.DEFAULT);
        try {
            super.emitBlockQuads(world, state, pos, randomSupplier, context);
        } finally {
            context.popTransform();
        }
    }

    @Override public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier,
            RenderContext context) {
        pushLedTransform(context, FrameColors.itemColor(stack, defaultColor), TeleportationFrameMonitorBlock.itemMonitorColor(stack));
        try {
            super.emitItemQuads(stack, randomSupplier, context);
        } finally {
            context.popTransform();
        }
    }
}
