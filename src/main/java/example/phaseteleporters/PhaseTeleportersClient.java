package example.phaseteleporters;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;

public final class PhaseTeleportersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PortalPlaneModel.register();
        HandledScreens.register(PhaseTeleportersMod.EMERGENCY_TELEPORT_SCREEN_HANDLER,
                EmergencyTeleportScreen<EmergencyTeleportScreenHandler>::new);
        HandledScreens.register(PhaseTeleportersMod.PORTABLE_EMERGENCY_SCREEN_HANDLER,
                EmergencyTeleportScreen<PortableEmergencyScreenHandler>::new);
        ItemDescriptionTooltip.register();
        HandledScreens.register(PhaseTeleportersMod.SOLAR_PANEL_SCREEN_HANDLER, SolarPanelScreen::new);
        TeleportationFrameMonitorRenderer.register();
        TeleportationFrameMonitorClientNetworking.register();
        EnergyCubeRenderer.register();
        QuantumTeleportRenderer.register();
        HandledScreens.register(PhaseTeleportersMod.QUANTUM_TELEPORT_SCREEN_HANDLER, QuantumTeleportScreen::new);
        QuantumTeleportClientNetworking.register();
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.INFUSION_STATION, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.ENRICHMENT_CHAMBER, RenderLayer.getCutout());
        HandledScreens.register(PhaseTeleportersMod.INFUSION_STATION_SCREEN_HANDLER, InfusionStationScreen::new);
        HandledScreens.register(PhaseTeleportersMod.CRUSHER_SCREEN_HANDLER, CrusherScreen::new);
        HandledScreens.register(PhaseTeleportersMod.ELECTRIC_FURNACE_SCREEN_HANDLER, ElectricFurnaceScreen::new);
        HandledScreens.register(PhaseTeleportersMod.ENRICHMENT_CHAMBER_SCREEN_HANDLER,
                EnrichmentChamberScreen::new);
        HandledScreens.register(PhaseTeleportersMod.COAL_GENERATOR_SCREEN_HANDLER, CoalGeneratorScreen::new);
        HandledScreens.register(PhaseTeleportersMod.CREATIVE_ENERGY_CUBE_SCREEN_HANDLER,
                CreativeEnergyCubeScreen::new);
        HandledScreens.register(PhaseTeleportersMod.ENERGY_CUBE_SCREEN_HANDLER, EnergyCubeScreen::new);
        HandledScreens.register(PhaseTeleportersMod.ENERGY_CONFIGURATION_SCREEN_HANDLER,
                EnergyConfigurationScreen::new);
        HandledScreens.register(PhaseTeleportersMod.TELEPORT_SCREEN_HANDLER, TeleportScreen::new);
        HandledScreens.register(PhaseTeleportersMod.PORTABLE_TELEPORT_SCREEN_HANDLER,
                PortableTeleportScreen::new);
        HandledScreens.register(PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT_SCREEN_HANDLER,
                InterdimensionalTeleportScreen::new);
        TeleportClientNetworking.register();
        PortableTeleportClientNetworking.register();
        InterdimensionalTeleportClientNetworking.register();
        InterdimensionalTerrainClient.register();
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.PORTAL_PLANE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(PhaseTeleportersMod.INTERDIMENSIONAL_PORTAL_PLANE,
                RenderLayer.getTranslucent());
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                tintIndex == 0 ? PortalColors.rgb(state.get(PortalPlaneBlock.COLOR)) : 0xFFFFFF,
                PhaseTeleportersMod.PORTAL_PLANE);
        ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
                tintIndex == 0 ? PortalColors.rgb(state.get(InterdimensionalPortalPlaneBlock.COLOR)) : 0xFFFFFF,
                PhaseTeleportersMod.INTERDIMENSIONAL_PORTAL_PLANE);
    }
}
