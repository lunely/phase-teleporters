package example.phaseteleports;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;

public final class PhaseTeleportsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(PhaseTeleportsMod.INFUSION_STATION_SCREEN_HANDLER, InfusionStationScreen::new);
        HandledScreens.register(PhaseTeleportsMod.CRUSHER_SCREEN_HANDLER, CrusherScreen::new);
        HandledScreens.register(PhaseTeleportsMod.ELECTRIC_FURNACE_SCREEN_HANDLER, ElectricFurnaceScreen::new);
        HandledScreens.register(PhaseTeleportsMod.COAL_GENERATOR_SCREEN_HANDLER, CoalGeneratorScreen::new);
        HandledScreens.register(PhaseTeleportsMod.CREATIVE_ENERGY_CUBE_SCREEN_HANDLER,
                CreativeEnergyCubeScreen::new);
        HandledScreens.register(PhaseTeleportsMod.ENERGY_CUBE_SCREEN_HANDLER, EnergyCubeScreen::new);
        HandledScreens.register(PhaseTeleportsMod.ENERGY_CONFIGURATION_SCREEN_HANDLER,
                EnergyConfigurationScreen::new);
        HandledScreens.register(PhaseTeleportsMod.TELEPORT_SCREEN_HANDLER, TeleportScreen::new);
        HandledScreens.register(PhaseTeleportsMod.PORTABLE_TELEPORT_SCREEN_HANDLER,
                PortableTeleportScreen::new);
        HandledScreens.register(PhaseTeleportsMod.INTERDIMENSIONAL_TELEPORT_SCREEN_HANDLER,
                InterdimensionalTeleportScreen::new);
        TeleportClientNetworking.register();
        PortableTeleportClientNetworking.register();
        InterdimensionalTeleportClientNetworking.register();
        InterdimensionalTerrainClient.register();
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportsMod.PORTAL_PLANE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportsMod.INTERDIMENSIONAL_PORTAL_PLANE,
                RenderLayer.getTranslucent());
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                tintIndex == 0 ? PortalColors.rgb(state.get(PortalPlaneBlock.COLOR)) : 0xFFFFFF,
                PhaseTeleportsMod.PORTAL_PLANE);
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                tintIndex == 0 ? PortalColors.rgb(state.get(InterdimensionalPortalPlaneBlock.COLOR)) : 0xFFFFFF,
                PhaseTeleportsMod.INTERDIMENSIONAL_PORTAL_PLANE);
    }
}
