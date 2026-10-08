package example.phaseteleporters;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/** Renders the separate port LED model in the opaque cutout pass at full brightness. */
public final class QuantumPortModel {
    private static final Identifier LED_MODEL = Identifier.of(PhaseTeleportersMod.MOD_ID,
            "block/quantum_port_led");
    private static final Random MODEL_RANDOM = Random.create(0);

    private QuantumPortModel() {}

    public static void register() {
        ModelLoadingPlugin.register(context -> context.addModels(LED_MODEL));
    }

    public static void renderLeds(MatrixStack matrices, VertexConsumerProvider consumers) {
        // Fetch after each resource reload so JSON and texture edits take effect together.
        var model = MinecraftClient.getInstance().getBakedModelManager().getModel(LED_MODEL);
        // Cutout writes depth. Cull the LED's back face so its ring is not
        // visible through the frame from inside the closed port housing.
        var vertices = consumers.getBuffer(
                RenderLayer.getEntityCutout(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE));
        // White vertex color preserves the RGB and alpha stored in the LED PNG.
        MODEL_RANDOM.setSeed(0);
        for (var quad : model.getQuads(null, null, MODEL_RANDOM)) {
            vertices.quad(matrices.peek(), quad, 1.0f, 1.0f, 1.0f, 1.0f,
                    LightmapTextureManager.MAX_LIGHT_COORDINATE,
                    OverlayTexture.DEFAULT_UV);
        }
    }
}
