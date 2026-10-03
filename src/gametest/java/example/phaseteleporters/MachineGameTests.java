package example.phaseteleporters;

import example.phaseteleporters.energy.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.test.*;
import net.minecraft.util.math.*;

public final class MachineGameTests extends PhaseGameTests {
    @GameTest(templateName = ROOM)
    public void cableRollbackAndSplitConserveEnergyAndQuotas(TestContext c) {
        BasicEnergyCableBlockEntity first = place(c, A, PhaseTeleportersMod.BASIC_ENERGY_CABLE);
        BasicEnergyCableBlockEntity middle = place(c, A.east(), PhaseTeleportersMod.BASIC_ENERGY_CABLE);
        BasicEnergyCableBlockEntity last = place(c, A.east(2), PhaseTeleportersMod.BASIC_ENERGY_CABLE);
        first.setLocalStored(100); middle.setLocalStored(100); last.setLocalStored(100);
        var api = team.reborn.energy.api.EnergyStorage.SIDED.find(c.getWorld(), first.getPos(), Direction.WEST);
        long tick = c.getWorld().getTime();
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(api.insert(50, tx), 50L, "Cable network accepts three hundred joules");
            c.assertEquals(first.getStored(), 600L, "Transactional insertion is visible");
        }
        c.assertEquals(first.getStored(), 300L, "Aborting restores all cable buffers");
        for (var cable : java.util.List.of(first, middle, last)) {
            c.assertEquals(cable.remainingTransfer(tick, true), BasicEnergyCableBlockEntity.TRANSFER_PER_TICK,
                    "Abort restores cable input quota");
            c.assertEquals(cable.remainingTransfer(tick, false), BasicEnergyCableBlockEntity.TRANSFER_PER_TICK,
                    "Abort restores cable output quota");
        }
        EnergyCableNetwork.distributeStored(c.getWorld(), first);
        for (var cable : java.util.List.of(first, middle, last))
            c.assertEquals(cable.processedTick(), tick, "Entire component marked processed");
        EnergyCableNetwork.distributeStored(c.getWorld(), last);
        c.assertEquals(first.getStored(), 300L, "Second component tick does not change stored energy");
        c.removeBlock(A.east());
        c.assertEquals(first.getStored() + last.getStored(), 300L, "Removing middle cable conserves energy when capacity remains");
        c.assertEquals(first.getCapacity(), 500L, "First segment has independent capacity");
        c.assertEquals(last.getCapacity(), 500L, "Second segment has independent capacity");
        c.assertEquals(middle.localStored(), 0L, "Removed cable does not retain redistributed energy");
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void infusionReloadPreservesOnlyMatchingRecipeProgress(TestContext c) {
        InfusionStationBlockEntity station = place(c, A, PhaseTeleportersMod.INFUSION_STATION);
        for (Direction side : Direction.values()) station.setSideMode(side, PESideMode.DISABLED);
        station.restoreStoredEnergy(20_000);
        station.setStack(0, new ItemStack(Items.IRON_INGOT));
        station.setStack(1, new ItemStack(Items.REDSTONE, 2));
        for (int i = 0; i < 25; i++) InfusionStationBlockEntity.tick(c.getWorld(), station.getPos(), station.getCachedState(), station);
        var lookup = c.getWorld().getRegistryManager();
        var saved = station.createNbt(lookup);
        c.assertEquals(saved.getInt("Progress"), 25, "Partial recipe progresses before save");
        station.read(saved, lookup);
        c.assertEquals(station.createNbt(lookup).getInt("Progress"), 25, "Same recipe retains progress after reload");
        station.setStack(0, new ItemStack(Items.GOLD_INGOT));
        station.read(station.createNbt(lookup), lookup);
        c.assertEquals(station.createNbt(lookup).getInt("Progress"), 0, "Changed input cannot inherit old recipe progress");
        for (int i = 0; i < InfusionStationBlockEntity.PROCESS_TIME - 1; i++)
            InfusionStationBlockEntity.tick(c.getWorld(), station.getPos(), station.getCachedState(), station);
        c.assertTrue(station.getStack(2).isEmpty(), "New recipe needs all one hundred ticks");
        InfusionStationBlockEntity.tick(c.getWorld(), station.getPos(), station.getCachedState(), station);
        item(c, station.getStack(2), PhaseTeleportersMod.BASIC_CONTROL_CIRCUIT, 1);
        c.assertEquals(station.getStored(), 10_000L, "Old partial work plus full new recipe costs ten thousand joules");
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void privateMachineMenusRespectOwnership(TestContext c) {
        var owner = player(c); var visitor = player(c);
        try {
            Block[] blocks = {PhaseTeleportersMod.COAL_GENERATOR, PhaseTeleportersMod.CRUSHER,
                    PhaseTeleportersMod.ELECTRIC_FURNACE, PhaseTeleportersMod.ENRICHMENT_CHAMBER,
                    PhaseTeleportersMod.INFUSION_STATION, PhaseTeleportersMod.SOLAR_PANEL,
                    PhaseTeleportersMod.ENERGY_CUBE, PhaseTeleportersMod.CREATIVE_ENERGY_CUBE};
            Vec3d position = Vec3d.ofCenter(c.getAbsolutePos(A)).add(0, 1, 0);
            owner.requestTeleport(position.x, position.y, position.z);
            visitor.requestTeleport(position.x, position.y, position.z);
            for (Block block : blocks) {
                c.removeBlock(A);
                PEBlockEntity machine = place(c, A, block);
                machine.assignOwner(owner); machine.setPublicAccess(false);
                var factory = (NamedScreenHandlerFactory) machine;
                var ownerMenu = factory.createMenu(1, owner.getInventory(), owner);
                var visitorMenu = factory.createMenu(2, visitor.getInventory(), visitor);
                c.assertTrue(ownerMenu.canUse(owner), "Owner may use private machine: " + block);
                c.assertFalse(visitorMenu.canUse(visitor), "Visitor cannot use private machine: " + block);
                c.assertFalse(PESecurity.canOpen(visitor, machine), "Opening private machine denied: " + block);
                var saved = machine.createNbt(c.getWorld().getRegistryManager());
                machine.setPublicAccess(true);
                c.assertTrue(visitorMenu.canUse(visitor), "Public access permits visitor: " + block);
                machine.read(saved, c.getWorld().getRegistryManager());
                c.assertFalse(visitorMenu.canUse(visitor), "Private access survives NBT reload: " + block);
                c.assertTrue(ownerMenu.canUse(owner), "Ownership survives NBT reload: " + block);
                ownerMenu.onClosed(owner); visitorMenu.onClosed(visitor);
            }
        } finally { disconnect(owner); disconnect(visitor); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void cubeMenusRejectReplacementAtSamePosition(TestContext c) {
        var player = player(c);
        try {
            Vec3d position = Vec3d.ofCenter(c.getAbsolutePos(A)).add(0, 1, 0);
            player.requestTeleport(position.x, position.y, position.z);
            for (Block block : new Block[]{PhaseTeleportersMod.ENERGY_CUBE, PhaseTeleportersMod.CREATIVE_ENERGY_CUBE}) {
                c.removeBlock(A);
                PEBlockEntity original = place(c, A, block);
                var menu = ((NamedScreenHandlerFactory) original).createMenu(1, player.getInventory(), player);
                c.assertTrue(menu.canUse(player), "Original cube menu is usable");
                c.removeBlock(A); PEBlockEntity replacement = place(c, A, block);
                c.assertFalse(menu.canUse(player), "Old menu cannot access replacement cube");
                PESideMode before = replacement.getSideMode(Direction.NORTH);
                c.assertFalse(menu.onButtonClick(player, EnergySideConfiguration.FIRST_BUTTON), "Old menu button rejected");
                c.assertEquals(replacement.getSideMode(Direction.NORTH), before, "Replacement configuration unchanged");
                menu.onClosed(player);
            }
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void staleEnergyApiHandleRejectsReplacedCube(TestContext c) {
        PEBlockEntity original = place(c, A, PhaseTeleportersMod.ENERGY_CUBE);
        original.setSideMode(Direction.WEST, PESideMode.INPUT_OUTPUT);
        original.restoreStoredEnergy(12_000);
        var cached = team.reborn.energy.api.EnergyStorage.SIDED.find(c.getWorld(), original.getPos(), Direction.WEST);
        c.assertEquals(cached.getAmount(), 2_000L, "Live cube exposes energy through API");
        c.removeBlock(A);
        PEBlockEntity replacement = place(c, A, PhaseTeleportersMod.ENERGY_CUBE);
        replacement.setSideMode(Direction.WEST, PESideMode.INPUT_OUTPUT);
        replacement.restoreStoredEnergy(6_000);
        c.assertFalse(cached.supportsInsertion(), "Broken cube no longer accepts API energy");
        c.assertFalse(cached.supportsExtraction(), "Broken cube no longer supplies API energy");
        c.assertEquals(cached.getAmount(), 0L, "Stale handle shows no stored energy");
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(cached.insert(100, tx), 0L, "Stale insertion rejected");
            c.assertEquals(cached.extract(100, tx), 0L, "Stale extraction rejected");
            tx.commit();
        }
        var live = team.reborn.energy.api.EnergyStorage.SIDED.find(c.getWorld(), replacement.getPos(), Direction.WEST);
        c.assertEquals(live.getAmount(), 1_000L, "New cube has an independent live API handle");
        c.complete();
    }

    @GameTest(templateName = ROOM, tickLimit = 30)
    public void directAutomationUsesBothDoubleChestHalves(TestContext c) {
        CrusherBlockEntity crusher = place(c, A, PhaseTeleportersMod.CRUSHER);
        crusher.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
        crusher.setSideMode(Direction.EAST, PESideMode.ITEM_OUTPUT);
        var left = Blocks.CHEST.getDefaultState().with(net.minecraft.block.ChestBlock.FACING, Direction.NORTH)
                .with(net.minecraft.block.ChestBlock.CHEST_TYPE, net.minecraft.block.enums.ChestType.LEFT);
        var right = left.with(net.minecraft.block.ChestBlock.CHEST_TYPE, net.minecraft.block.enums.ChestType.RIGHT);
        c.setBlockState(A.west(2), left); c.setBlockState(A.west(), right);
        c.setBlockState(A.east(), left); c.setBlockState(A.east(2), right);
        net.minecraft.block.entity.ChestBlockEntity source = c.getBlockEntity(A.west(2));
        net.minecraft.block.entity.ChestBlockEntity nearTarget = c.getBlockEntity(A.east());
        net.minecraft.block.entity.ChestBlockEntity farTarget = c.getBlockEntity(A.east(2));
        source.setStack(0, new ItemStack(Blocks.OBSIDIAN, 2));
        for (int slot = 0; slot < nearTarget.size(); slot++) nearTarget.setStack(slot, new ItemStack(Items.DIRT, 64));
        for (int slot = 1; slot < farTarget.size(); slot++) farTarget.setStack(slot, new ItemStack(Items.DIRT, 64));
        crusher.setStack(1, new ItemStack(PhaseTeleportersMod.OBSIDIAN_DUST));
        c.addInstantFinalTask(() -> {
            item(c, crusher.getStack(0), Blocks.OBSIDIAN.asItem(), 2);
            c.assertTrue(source.getStack(0).isEmpty(), "Input taken from far chest half");
            item(c, farTarget.getStack(0), PhaseTeleportersMod.OBSIDIAN_DUST, 1);
            c.assertTrue(crusher.getStack(1).isEmpty(), "Output reaches far half when near half is full");
        });
    }

    @GameTest(templateName = ROOM)
    public void menusExposeWorkingBatterySlots(TestContext c) {
        var player = player(c);
        try {
            Block[] machines = {PhaseTeleportersMod.CRUSHER, PhaseTeleportersMod.ENRICHMENT_CHAMBER,
                    PhaseTeleportersMod.ELECTRIC_FURNACE, PhaseTeleportersMod.INFUSION_STATION,
                    PhaseTeleportersMod.COAL_GENERATOR, PhaseTeleportersMod.SOLAR_PANEL};
            for (int i = 0; i < machines.length; i++) {
                BlockEntity entity = place(c, new BlockPos(2 + i * 2, 1, 8), machines[i]);
                Inventory inventory = (Inventory) entity;
                var menu = ((NamedScreenHandlerFactory) entity).createMenu(1, player.getInventory(), player);
                c.assertTrue(menu != null, "Menu must open: " + machines[i]);
                c.assertTrue(menu.slots.stream().anyMatch(slot -> slot.inventory == inventory
                        && slot.getIndex() == inventory.size() - 1 && slot.canInsert(new ItemStack(PhaseTeleportersMod.BATTERY))),
                        "Menu must expose a working battery slot: " + machines[i]);
            }
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM, tickLimit = 240)
    public void recipesRunOnBatteriesAndConserveEnergy(TestContext c) {
        CrusherBlockEntity crusher = place(c, new BlockPos(2, 1, 3), PhaseTeleportersMod.CRUSHER);
        EnrichmentChamberBlockEntity enrichment = place(c, new BlockPos(5, 1, 3), PhaseTeleportersMod.ENRICHMENT_CHAMBER);
        ElectricFurnaceBlockEntity furnace = place(c, new BlockPos(8, 1, 3), PhaseTeleportersMod.ELECTRIC_FURNACE);
        InfusionStationBlockEntity infusion = place(c, new BlockPos(11, 1, 3), PhaseTeleportersMod.INFUSION_STATION);
        Inventory[] machines = {crusher, enrichment, furnace, infusion};
        for (Inventory machine : machines) machine.setStack(machine.size() - 1, chargedBattery());
        crusher.setStack(0, new ItemStack(Items.OBSIDIAN));
        enrichment.setStack(0, new ItemStack(Items.DIAMOND));
        furnace.setStack(0, new ItemStack(Items.IRON_ORE));
        infusion.setStack(0, new ItemStack(Items.IRON_INGOT)); infusion.setStack(1, new ItemStack(Items.COAL));
        c.runAtTick(200, () -> {
            item(c, crusher.getStack(1), PhaseTeleportersMod.OBSIDIAN_DUST, 1);
            item(c, enrichment.getStack(1), PhaseTeleportersMod.ENRICHED_DIAMOND, 1);
            item(c, furnace.getStack(1), Items.IRON_INGOT, 1);
            item(c, infusion.getStack(2), PhaseTeleportersMod.STEEL_INGOT, 1);
            long[] costs = {5_000, 6_000, 3_200, 8_000};
            for (int i = 0; i < machines.length; i++) {
                c.assertEquals(((PEBlockEntity) machines[i]).getStored()
                        + batteryItem().getStoredPE(machines[i].getStack(machines[i].size() - 1)),
                        BatteryItem.CAPACITY - costs[i], "Battery plus buffer balances after one recipe");
                c.assertTrue(machines[i].getStack(0).isEmpty(), "Exactly one input consumed");
            }
            c.complete();
        });
    }

    @GameTest(templateName = ROOM)
    public void automationFiltersRecipeInputsAndHidesBatterySlots(TestContext c) {
        Block[] blocks = {PhaseTeleportersMod.CRUSHER, PhaseTeleportersMod.ENRICHMENT_CHAMBER,
                PhaseTeleportersMod.ELECTRIC_FURNACE, PhaseTeleportersMod.INFUSION_STATION, PhaseTeleportersMod.COAL_GENERATOR};
        Item[] valid = {Items.OBSIDIAN, Items.DIAMOND, Items.IRON_ORE, Items.IRON_INGOT, Items.COAL};
        for (int i = 0; i < blocks.length; i++) {
            PEBlockEntity entity = place(c, new BlockPos(2 + i * 2, 1, 8), blocks[i]);
            entity.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
            var storage = ItemStorage.SIDED.find(c.getWorld(), entity.getPos(), Direction.WEST);
            c.assertTrue(storage != null, "Input discoverable through Transfer API");
            try (var tx = Transaction.openOuter()) {
                c.assertEquals(storage.insert(ItemVariant.of(Items.DIRT), 64, tx), 0L, "Dirt rejected");
                c.assertEquals(storage.insert(ItemVariant.of(PhaseTeleportersMod.BATTERY), 1, tx), 0L, "Cargo cannot enter battery slot");
                c.assertEquals(storage.insert(ItemVariant.of(valid[i]), 1, tx), 1L, "Recipe input accepted"); tx.commit();
            }
            item(c, ((Inventory) entity).getStack(0), valid[i], 1);
        }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void blockedOutputDoesNotConsumeInputOrEnergy(TestContext c) {
        CrusherBlockEntity crusher = place(c, A, PhaseTeleportersMod.CRUSHER);
        crusher.restoreStoredEnergy(10_000);
        crusher.setStack(0, new ItemStack(Items.OBSIDIAN, 2));
        crusher.setStack(1, new ItemStack(PhaseTeleportersMod.OBSIDIAN_DUST, 64));
        for (int i = 0; i < 110; i++) CrusherBlockEntity.tick(c.getWorld(), crusher.getPos(), crusher.getCachedState(), crusher);
        item(c, crusher.getStack(0), Items.OBSIDIAN, 2);
        c.assertEquals(crusher.getStored(), 10_000L, "Blocked output does not spend energy"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void coalBurnsAtFullCapacityAndChargesBattery(TestContext c) {
        CoalGeneratorBlockEntity generator = place(c, A, PhaseTeleportersMod.COAL_GENERATOR);
        generator.restoreStoredEnergy(generator.getCapacity()); generator.setStack(0, new ItemStack(Items.COAL, 2));
        for (int i = 0; i < 301; i++) CoalGeneratorBlockEntity.tick(c.getWorld(), generator.getPos(), generator.getCachedState(), generator);
        c.assertTrue(generator.getStack(0).isEmpty(), "Fuel continues burning at full capacity");
        c.assertEquals(generator.getStored(), generator.getCapacity(), "Buffer does not overflow");
        var battery = new ItemStack(PhaseTeleportersMod.BATTERY); generator.setStack(1, battery);
        PEEnergyItemTransfer.charge(generator, battery);
        c.assertEquals(batteryItem().getStoredPE(battery), CoalGeneratorBlockEntity.MAX_OUTPUT_PER_TICK, "Generator charges battery");
        c.complete();
    }

    @GameTest(templateName = ROOM, batchId = "solar_weather", skyAccess = true)
    public void solarDayRainNightAndRoof(TestContext c) {
        var world = c.getWorld(); long time = world.getTimeOfDay();
        boolean rain = world.getLevelProperties().isRaining(), thunder = world.getLevelProperties().isThundering();
        float rainGradient = world.getRainGradient(1), thunderGradient = world.getThunderGradient(1);
        try {
            SolarPanelBlockEntity panel = place(c, A, PhaseTeleportersMod.SOLAR_PANEL);
            world.setTimeOfDay(6_000); world.setWeather(0, 0, false, false); world.setRainGradient(0); world.setThunderGradient(0);
            c.assertEquals(SolarPanelBlockEntity.generation(world, panel.getPos()), 250, "Clear daylight output");
            world.setWeather(0, 1000, true, false); world.setRainGradient(1);
            c.assertEquals(SolarPanelBlockEntity.generation(world, panel.getPos()), 110, "Rain output");
            world.setTimeOfDay(18_000);
            c.assertEquals(SolarPanelBlockEntity.generation(world, panel.getPos()), 0, "Night output");
            world.setTimeOfDay(6_000); c.setBlockState(A.up(3), Blocks.STONE);
            c.assertEquals(panel.getCapacity(), 5_000L, "Buffer capacity");
            // Sky visibility uses the light engine, which updates after block placement.
            c.runAtTick(5, () -> {
                try {
                    world.setTimeOfDay(6_000);
                    c.assertEquals(SolarPanelBlockEntity.generation(world, panel.getPos()), 0, "Roof blocks generation");
                    c.complete();
                } finally {
                    world.setTimeOfDay(time); world.setWeather(0, 0, rain, thunder);
                    world.setRainGradient(rainGradient); world.setThunderGradient(thunderGradient);
                }
            });
            return;
        } finally {
            world.setTimeOfDay(time); world.setWeather(0, 0, rain, thunder);
            world.setRainGradient(rainGradient); world.setThunderGradient(thunderGradient);
        }
    }

    @GameTest(templateName = ROOM)
    public void batteryAndSettingsSurviveNbt(TestContext c) {
        ElectricFurnaceBlockEntity original = place(c, A, PhaseTeleportersMod.ELECTRIC_FURNACE);
        original.setStack(2, chargedBattery()); original.restoreStoredEnergy(1_234);
        original.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT); original.setRedstoneMode(PERedstoneMode.WITH_SIGNAL);
        NbtCompound data = original.createNbt(c.getWorld().getRegistryManager());
        var restored = new ElectricFurnaceBlockEntity(original.getPos(), original.getCachedState()); restored.setWorld(c.getWorld());
        restored.read(data, c.getWorld().getRegistryManager());
        c.assertEquals(restored.getStored(), 1_234L, "Energy survives reload");
        c.assertEquals(batteryItem().getStoredPE(restored.getStack(2)), BatteryItem.CAPACITY, "Battery charge survives reload");
        c.assertEquals(restored.getSideMode(Direction.WEST), PESideMode.ITEM_INPUT, "Side survives reload");
        c.assertEquals(restored.getRedstoneMode(), PERedstoneMode.WITH_SIGNAL, "Redstone mode survives reload"); c.complete();
    }
}
