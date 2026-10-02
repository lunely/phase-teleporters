package example.phaseteleporters;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import example.phaseteleporters.energy.PEChargeableItem;

public final class PhaseTeleportersMod implements ModInitializer {
    public static final String MOD_ID = "phaseteleporters";
    // Vanilla bare-hand breaking takes 1.5 seconds per hardness point.
    private static final float BLOCK_HARDNESS = 3.5f - 2.0f / 1.5f;

    public static final Item STEEL_INGOT = register("steel_ingot");
    public static final Item GLOWSTONE_INGOT = register("glowstone_ingot");
    public static final Block MACHINE_CASING = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "machine_casing"),
            new Block(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item MACHINE_CASING_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "machine_casing"),
            new BlockItem(MACHINE_CASING, new Item.Settings()));
    public static final Item BASIC_ALLOY = register("basic_alloy");
    public static final Item ADVANCED_ALLOY = register("advanced_alloy");
    public static final Item PHASE_ALLOY = register("phase_alloy");
    public static final Item OBSIDIAN_DUST = register("obsidian_dust");
    public static final Item PURIFIED_OBSIDIAN_DUST = register("purified_obsidian_dust");
    public static final Item ENRICHED_PURIFIED_OBSIDIAN_DUST = register("enriched_purified_obsidian_dust");
    public static final Item ENRICHED_DIAMOND = register("enriched_diamond");
    public static final Item ENRICHED_REDSTONE_DUST = register("enriched_redstone_dust");
    public static final Item ENRICHED_COAL = register("enriched_coal");
    public static final Item REFINED_OBSIDIAN_INGOT = register("refined_obsidian_ingot");
    public static final Item TELEPORTATION_CORE = register("teleportation_core");
    public static final Item BASIC_CONTROL_CIRCUIT = register("basic_control_circuit");
    public static final Item PHASE_CONTROL_CIRCUIT = register("phase_control_circuit");
    public static final Item SOLAR_ELEMENT = register("solar_element");
    public static final Item ANCHOR_UPGRADE = register("anchor_upgrade");
    public static final Item PORTABLE_TELEPORT = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "portable_teleporter"),
            new PortableTeleportItem(new Item.Settings().maxCount(1)));
    public static final Item BATTERY = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "battery"),
            new BatteryItem(new Item.Settings().maxCount(1)));
    public static final Block INFUSION_STATION = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "infusion_station"),
            new InfusionStationBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item INFUSION_STATION_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "infusion_station"),
            new BlockItem(INFUSION_STATION, new Item.Settings()));
    public static final Block CRUSHER = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "crusher"),
            new CrusherBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item CRUSHER_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "crusher"),
            new BlockItem(CRUSHER, new Item.Settings()));
    public static final Block ELECTRIC_FURNACE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "electric_furnace"),
            new ElectricFurnaceBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item ELECTRIC_FURNACE_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "electric_furnace"),
            new BlockItem(ELECTRIC_FURNACE, new Item.Settings()));
    public static final Block ENRICHMENT_CHAMBER = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "enrichment_chamber"),
            new EnrichmentChamberBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f).nonOpaque()
                    .luminance(state -> state.get(EnrichmentChamberBlock.LIT) ? 5 : 0)));
    public static final Item ENRICHMENT_CHAMBER_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "enrichment_chamber"),
            new BlockItem(ENRICHMENT_CHAMBER, new Item.Settings()));
    public static final Block SOLAR_PANEL = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "solar_panel"),
            new SolarPanelBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item SOLAR_PANEL_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "solar_panel"),
            new BlockItem(SOLAR_PANEL, new Item.Settings()));
    public static final Block COAL_GENERATOR = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "coal_generator"),
            new CoalGeneratorBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item COAL_GENERATOR_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "coal_generator"),
            new BlockItem(COAL_GENERATOR, new Item.Settings()));
    public static final Block CREATIVE_ENERGY_CUBE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "creative_energy_cube"),
            new CreativeEnergyCubeBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f).nonOpaque()));
    public static final Item CREATIVE_ENERGY_CUBE_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "creative_energy_cube"),
            new CreativeEnergyCubeItem(CREATIVE_ENERGY_CUBE, new Item.Settings()));
    public static final Block ENERGY_CUBE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "energy_cube"),
            new EnergyCubeBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f).nonOpaque()));
    public static final Item ENERGY_CUBE_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "energy_cube"),
            new EnergyCubeItem(ENERGY_CUBE, new Item.Settings()));
    public static final Block BASIC_ENERGY_CABLE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "basic_wire"),
            new BasicEnergyCableBlock(AbstractBlock.Settings.create().strength(0.8f)
                    .sounds(BlockSoundGroup.WOOL).nonOpaque()));
    public static final Item BASIC_ENERGY_CABLE_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "basic_wire"),
            new BlockItem(BASIC_ENERGY_CABLE, new Item.Settings()));
    public static final Block TELEPORTATION_FRAME = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "teleportation_frame"),
            new Block(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item TELEPORTATION_FRAME_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "teleportation_frame"),
            new BlockItem(TELEPORTATION_FRAME, new Item.Settings()));
    public static final Block TELEPORTATION_FRAME_MONITOR = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "teleportation_frame_monitor"),
            new TeleportationFrameMonitorBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item TELEPORTATION_FRAME_MONITOR_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "teleportation_frame_monitor"),
            new BlockItem(TELEPORTATION_FRAME_MONITOR, new Item.Settings()));
    public static final BlockEntityType<TeleportationFrameMonitorBlockEntity> TELEPORTATION_FRAME_MONITOR_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "teleportation_frame_monitor"),
                    BlockEntityType.Builder.create(TeleportationFrameMonitorBlockEntity::new,
                            TELEPORTATION_FRAME_MONITOR).build(null));
    public static final Block TELEPORT = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "local_teleporter"),
            new TeleportBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item TELEPORT_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "local_teleporter"),
            new BlockItem(TELEPORT, new Item.Settings()));
    public static final Block PORTAL_PLANE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "portal_plane"),
            new PortalPlaneBlock(AbstractBlock.Settings.create().noCollision().nonOpaque().dropsNothing().strength(-1.0f)));
    public static final Block QUANTUM_TELEPORT = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "quantum_teleporter"),
            new QuantumTeleportBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)
                    .nonOpaque()));
    public static final Item QUANTUM_TELEPORT_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "quantum_teleporter"),
            new BlockItem(QUANTUM_TELEPORT, new Item.Settings()));
    public static final BlockEntityType<QuantumTeleportBlockEntity> QUANTUM_TELEPORT_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "quantum_teleporter"),
                    BlockEntityType.Builder.create(QuantumTeleportBlockEntity::new, QUANTUM_TELEPORT).build(null));
    public static final ScreenHandlerType<QuantumTeleportScreenHandler> QUANTUM_TELEPORT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "quantum_teleporter"),
                    new ScreenHandlerType<>(QuantumTeleportScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final Block EMERGENCY_TELEPORT = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "emergency_teleporter"),
            new EmergencyTeleportBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f).nonOpaque()));
    public static final Item EMERGENCY_TELEPORT_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "emergency_teleporter"),
            new BlockItem(EMERGENCY_TELEPORT, new Item.Settings()));
    public static final BlockEntityType<EmergencyTeleportBlockEntity> EMERGENCY_TELEPORT_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "emergency_teleporter"),
                    BlockEntityType.Builder.create(EmergencyTeleportBlockEntity::new, EMERGENCY_TELEPORT).build(null));
    public static final ScreenHandlerType<EmergencyTeleportScreenHandler> EMERGENCY_TELEPORT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "emergency_teleporter"),
                    new ScreenHandlerType<>(EmergencyTeleportScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final ScreenHandlerType<PortableEmergencyScreenHandler> PORTABLE_EMERGENCY_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "portable_emergency"),
                    new ScreenHandlerType<>(PortableEmergencyScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final Block INTERDIMENSIONAL_TELEPORTATION_FRAME = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "interdimensional_teleportation_frame"),
            new Block(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item INTERDIMENSIONAL_TELEPORTATION_FRAME_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "interdimensional_teleportation_frame"),
            new BlockItem(INTERDIMENSIONAL_TELEPORTATION_FRAME, new Item.Settings()));
    public static final Block INTERDIMENSIONAL_TELEPORT = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "interdimensional_teleporter"),
            new InterdimensionalTeleportBlock(AbstractBlock.Settings.create().strength(BLOCK_HARDNESS, 3.5f)));
    public static final Item INTERDIMENSIONAL_TELEPORT_ITEM = Registry.register(
            Registries.ITEM, Identifier.of(MOD_ID, "interdimensional_teleporter"),
            new BlockItem(INTERDIMENSIONAL_TELEPORT, new Item.Settings()));
    public static final Block INTERDIMENSIONAL_PORTAL_PLANE = Registry.register(
            Registries.BLOCK, Identifier.of(MOD_ID, "interdimensional_portal_plane"),
            new InterdimensionalPortalPlaneBlock(AbstractBlock.Settings.create()
                    .noCollision().nonOpaque().dropsNothing().strength(-1.0f)));
    public static final BlockEntityType<InfusionStationBlockEntity> INFUSION_STATION_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "infusion_station"),
                    BlockEntityType.Builder.create(InfusionStationBlockEntity::new, INFUSION_STATION).build(null));
    public static final ScreenHandlerType<InfusionStationScreenHandler> INFUSION_STATION_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "infusion_station"),
                    new ScreenHandlerType<>(InfusionStationScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<CrusherBlockEntity> CRUSHER_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "crusher"),
                    BlockEntityType.Builder.create(CrusherBlockEntity::new, CRUSHER).build(null));
    public static final ScreenHandlerType<CrusherScreenHandler> CRUSHER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "crusher"),
                    new ScreenHandlerType<>(CrusherScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "electric_furnace"),
                    BlockEntityType.Builder.create(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE).build(null));
    public static final ScreenHandlerType<ElectricFurnaceScreenHandler> ELECTRIC_FURNACE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "electric_furnace"),
                    new ScreenHandlerType<>(ElectricFurnaceScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final RecipeType<EnrichmentRecipe> ENRICHMENT_RECIPE_TYPE = Registry.register(
            Registries.RECIPE_TYPE, Identifier.of(MOD_ID, "enrichment"),
            new RecipeType<>() {
                @Override public String toString() { return MOD_ID + ":enrichment"; }
            });
    public static final RecipeSerializer<EnrichmentRecipe> ENRICHMENT_RECIPE_SERIALIZER = Registry.register(
            Registries.RECIPE_SERIALIZER, Identifier.of(MOD_ID, "enrichment"),
            new EnrichmentRecipe.Serializer());
    public static final BlockEntityType<EnrichmentChamberBlockEntity> ENRICHMENT_CHAMBER_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "enrichment_chamber"),
                    BlockEntityType.Builder.create(EnrichmentChamberBlockEntity::new, ENRICHMENT_CHAMBER).build(null));
    public static final ScreenHandlerType<EnrichmentChamberScreenHandler> ENRICHMENT_CHAMBER_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "enrichment_chamber"),
                    new ScreenHandlerType<>(EnrichmentChamberScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<SolarPanelBlockEntity> SOLAR_PANEL_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "solar_panel"),
                    BlockEntityType.Builder.create(SolarPanelBlockEntity::new, SOLAR_PANEL).build(null));
    public static final ScreenHandlerType<SolarPanelScreenHandler> SOLAR_PANEL_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "solar_panel"),
                    new ScreenHandlerType<>(SolarPanelScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<CoalGeneratorBlockEntity> COAL_GENERATOR_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "coal_generator"),
                    BlockEntityType.Builder.create(CoalGeneratorBlockEntity::new, COAL_GENERATOR).build(null));
    public static final ScreenHandlerType<CoalGeneratorScreenHandler> COAL_GENERATOR_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "coal_generator"),
                    new ScreenHandlerType<>(CoalGeneratorScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<CreativeEnergyCubeBlockEntity> CREATIVE_ENERGY_CUBE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "creative_energy_cube"),
                    BlockEntityType.Builder.create(CreativeEnergyCubeBlockEntity::new, CREATIVE_ENERGY_CUBE).build(null));
    public static final ScreenHandlerType<CreativeEnergyCubeScreenHandler> CREATIVE_ENERGY_CUBE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "creative_energy_cube"),
                    new ScreenHandlerType<>(CreativeEnergyCubeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<EnergyCubeBlockEntity> ENERGY_CUBE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "energy_cube"),
                    BlockEntityType.Builder.create(EnergyCubeBlockEntity::new, ENERGY_CUBE).build(null));
    public static final ScreenHandlerType<EnergyCubeScreenHandler> ENERGY_CUBE_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "energy_cube"),
                    new ScreenHandlerType<>(EnergyCubeScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final ScreenHandlerType<EnergyConfigurationScreenHandler> ENERGY_CONFIGURATION_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "energy_configuration"),
                    new ScreenHandlerType<>(EnergyConfigurationScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<BasicEnergyCableBlockEntity> BASIC_ENERGY_CABLE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "basic_wire"),
                    BlockEntityType.Builder.create(BasicEnergyCableBlockEntity::new, BASIC_ENERGY_CABLE).build(null));
    public static final BlockEntityType<TeleportBlockEntity> TELEPORT_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "local_teleporter"),
                    BlockEntityType.Builder.create(TeleportBlockEntity::new, TELEPORT).build(null));
    public static final ScreenHandlerType<TeleportScreenHandler> TELEPORT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "local_teleporter"),
                    new ScreenHandlerType<>(TeleportScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final ScreenHandlerType<PortableTeleportScreenHandler> PORTABLE_TELEPORT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "portable_teleporter"),
                    new ScreenHandlerType<>(PortableTeleportScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<PortalPlaneBlockEntity> PORTAL_PLANE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "portal_plane"),
                    BlockEntityType.Builder.create(PortalPlaneBlockEntity::new, PORTAL_PLANE).build(null));
    public static final BlockEntityType<InterdimensionalTeleportBlockEntity> INTERDIMENSIONAL_TELEPORT_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "interdimensional_teleporter"),
                    BlockEntityType.Builder.create(InterdimensionalTeleportBlockEntity::new,
                            INTERDIMENSIONAL_TELEPORT).build(null));
    public static final ScreenHandlerType<InterdimensionalTeleportScreenHandler> INTERDIMENSIONAL_TELEPORT_SCREEN_HANDLER =
            Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "interdimensional_teleporter"),
                    new ScreenHandlerType<>(InterdimensionalTeleportScreenHandler::new, FeatureFlags.VANILLA_FEATURES));
    public static final BlockEntityType<InterdimensionalPortalPlaneBlockEntity> INTERDIMENSIONAL_PORTAL_PLANE_BLOCK_ENTITY =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MOD_ID, "interdimensional_portal_plane"),
                    BlockEntityType.Builder.create(InterdimensionalPortalPlaneBlockEntity::new,
                            INTERDIMENSIONAL_PORTAL_PLANE).build(null));

    private static Item register(String name) {
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), new Item(new Item.Settings()));
    }

    @Override
    public void onInitialize() {
        EmergencyTeleportRescue.register();
        TeleportationFrameMonitorNetworking.registerServer();
        example.phaseteleporters.energy.EnergyApiCompat.register();
        QuantumItemStorage.register();
        QuantumFluidStorage.register();
        TeleportNetworking.registerServer();
        InterdimensionalTeleportNetworking.registerServer();
        QuantumTeleportNetworking.registerServer();
        PortableTeleportNetworking.registerServer();
        PortalReentryGuard.register();
        InterdimensionalReentryGuard.register();
        EntityPortalReentryGuard.register();
        PortalCollisionTeleport.register();
        AnchorChunkState.register();
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MOD_ID, "main"),
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(TELEPORTATION_CORE))
                        .displayName(Text.translatable("itemGroup.phaseteleporters.main"))
                        .entries((context, entries) -> {
                            entries.add(STEEL_INGOT);
                            entries.add(GLOWSTONE_INGOT);
                            entries.add(BASIC_ALLOY);
                            entries.add(ADVANCED_ALLOY);
                            entries.add(PHASE_ALLOY);
                            entries.add(OBSIDIAN_DUST);
                            entries.add(PURIFIED_OBSIDIAN_DUST);
                            entries.add(ENRICHED_PURIFIED_OBSIDIAN_DUST);
                            entries.add(ENRICHED_DIAMOND);
                            entries.add(ENRICHED_REDSTONE_DUST);
                            entries.add(ENRICHED_COAL);
                            entries.add(REFINED_OBSIDIAN_INGOT);
                            entries.add(TELEPORTATION_CORE);
                            entries.add(BASIC_CONTROL_CIRCUIT);
                            entries.add(PHASE_CONTROL_CIRCUIT);
                            entries.add(SOLAR_ELEMENT);
                            entries.add(ANCHOR_UPGRADE);
                            entries.add(PORTABLE_TELEPORT);
                            entries.add(fullyCharged(PORTABLE_TELEPORT));
                            entries.add(BATTERY);
                            entries.add(fullyCharged(BATTERY));
                            entries.add(MACHINE_CASING_ITEM);
                            entries.add(INFUSION_STATION_ITEM);
                            entries.add(CRUSHER_ITEM);
                            entries.add(ELECTRIC_FURNACE_ITEM);
                            entries.add(ENRICHMENT_CHAMBER_ITEM);
                            entries.add(COAL_GENERATOR_ITEM);
                            entries.add(SOLAR_PANEL_ITEM);
                            entries.add(CREATIVE_ENERGY_CUBE_ITEM);
                            entries.add(ENERGY_CUBE_ITEM);
                             ItemStack chargedEnergyCube = new ItemStack(ENERGY_CUBE_ITEM);
                             EnergyCubeItem.setStoredPE(chargedEnergyCube, EnergyCubeBlockEntity.CAPACITY);
                             entries.add(chargedEnergyCube);
                            entries.add(BASIC_ENERGY_CABLE_ITEM);
                            entries.add(TELEPORTATION_FRAME_ITEM);
                            entries.add(TELEPORTATION_FRAME_MONITOR_ITEM);
                            entries.add(TELEPORT_ITEM);
                            entries.add(INTERDIMENSIONAL_TELEPORT_ITEM);
                            entries.add(QUANTUM_TELEPORT_ITEM);
                            entries.add(EMERGENCY_TELEPORT_ITEM);
                        }).build());
    }

    private static ItemStack fullyCharged(Item item) {
        ItemStack stack = new ItemStack(item);
        if (item instanceof PEChargeableItem chargeable)
            chargeable.insertPE(stack, chargeable.getCapacityPE(stack), false);
        return stack;
    }
}
