package example.phaseteleporters;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Item;
import net.minecraft.inventory.Inventories;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameMode;
import example.phaseteleporters.energy.PEChargeableItem;
import example.phaseteleporters.energy.PEChargingCubeBlockEntity;

public final class ColorConfiguratorGameTests extends PhaseGameTests {
    private static ColorConfiguratorItem configurator() {
        return (ColorConfiguratorItem) PhaseTeleportersMod.COLOR_CONFIGURATOR;
    }

    private static ActionResult click(TestContext context, ServerPlayerEntity player, BlockPos relative) {
        BlockPos pos = context.getAbsolutePos(relative);
        return configurator().useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false)));
    }

    @GameTest(templateName = ROOM)
    public void colorsFollowUiOrderOnBothFrameTypesAndWrap(TestContext context) {
        var player = player(context);
        try {
            ItemStack stack = new ItemStack(PhaseTeleportersMod.COLOR_CONFIGURATOR);
            configurator().insertPE(stack, ColorConfiguratorItem.CAPACITY, false);
            player.setStackInHand(Hand.MAIN_HAND, stack);
            long remaining = ColorConfiguratorItem.CAPACITY;
            for (Block frame : new Block[]{PhaseTeleportersMod.TELEPORTATION_FRAME,
                    PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORTATION_FRAME}) {
                context.setBlockState(A, frame.getDefaultState().with(
                        TeleportationFrameBlock.COLOR, PortalColors.displayColor(0)));
                for (int index = 0; index < PortalColors.count(); index++) {
                    context.assertEquals(click(context, player, A), ActionResult.CONSUME,
                            "Server accepts one color change");
                    remaining -= ColorConfiguratorItem.ENERGY_PER_CHANGE;
                    context.assertEquals(configurator().getStoredPE(stack), remaining,
                            "Each click costs exactly 100 energy");
                    context.assertEquals(context.getWorld().getBlockState(context.getAbsolutePos(A))
                                    .get(TeleportationFrameBlock.COLOR),
                            PortalColors.displayColor((index + 1) % PortalColors.count()),
                            "Frame follows the UI palette and wraps at its end");
                }
            }
        } finally {
            disconnect(player);
        }
        context.complete();
    }

    @GameTest(templateName = ROOM)
    public void insufficientEnergyAndWrongTargetsDoNotChangeAnything(TestContext context) {
        var player = player(context);
        try {
            ItemStack stack = new ItemStack(PhaseTeleportersMod.COLOR_CONFIGURATOR);
            configurator().insertPE(stack, 99, false);
            player.setStackInHand(Hand.MAIN_HAND, stack);
            context.setBlockState(A, PhaseTeleportersMod.TELEPORTATION_FRAME);
            context.assertEquals(click(context, player, A), ActionResult.FAIL, "99 energy is insufficient");
            context.assertEquals(configurator().getStoredPE(stack), 99L, "Failed click spends no energy");
            context.assertEquals(context.getWorld().getBlockState(context.getAbsolutePos(A))
                    .get(TeleportationFrameBlock.COLOR), FrameColors.DEFAULT, "Failed click keeps the frame color");

            configurator().insertPE(stack, 1, false);
            context.setBlockState(B, Blocks.STONE);
            context.assertEquals(click(context, player, B), ActionResult.PASS, "Unrelated blocks are ignored");
            context.assertEquals(configurator().getStoredPE(stack), 100L, "Wrong target spends no energy");
            player.changeGameMode(GameMode.CREATIVE);
            context.assertEquals(click(context, player, A), ActionResult.CONSUME, "Exactly 100 energy is sufficient");
            context.assertEquals(configurator().getStoredPE(stack), 0L, "Creative clicks also spend exactly 100 energy");
            context.assertEquals(context.getWorld().getBlockState(context.getAbsolutePos(A))
                    .get(TeleportationFrameBlock.COLOR), PortalColors.nextDisplayColor(FrameColors.DEFAULT),
                    "Successful click advances one color");
            context.assertEquals(click(context, player, A), ActionResult.FAIL, "An empty configurator cannot recolor");
        } finally {
            disconnect(player);
        }
        context.complete();
    }

    @GameTest(templateName = ROOM)
    public void energyCubeChargesConfiguratorToCapacityAndEnergySurvivesNbt(TestContext context) {
        EnergyCubeBlockEntity cube = place(context, A, PhaseTeleportersMod.ENERGY_CUBE);
        ItemStack stack = new ItemStack(PhaseTeleportersMod.COLOR_CONFIGURATOR);
        context.assertEquals(configurator().getCapacityPE(stack), 200_000L, "Capacity is 200k energy");
        cube.restoreStoredEnergy(300_000);
        cube.setStack(0, stack);
        EnergyCubeBlockEntity.tick(context.getWorld(), cube.getPos(), cube.getCachedState(), cube);
        context.assertEquals(configurator().getStoredPE(stack), 200_000L, "Cube fills the configurator");
        context.assertEquals(cube.getStored(), 100_000L, "Charging conserves energy");
        context.assertEquals(configurator().insertPE(stack, 1, false), 0L, "Capacity cannot overflow");
        var lookup = context.getWorld().getRegistryManager();
        ItemStack loaded = ItemStack.fromNbt(lookup, stack.encode(lookup)).orElseThrow();
        context.assertEquals(configurator().getStoredPE(loaded), 200_000L, "Charge survives save and load");
        context.complete();
    }

    @GameTest(templateName = ROOM)
    public void cubesAcceptOnlyBatteriesForDischargingAndShiftClickChargesTools(TestContext context) {
        var player = player(context);
        try {
            for (Block block : new Block[]{PhaseTeleportersMod.ENERGY_CUBE, PhaseTeleportersMod.CREATIVE_ENERGY_CUBE}) {
                context.removeBlock(A);
                PEChargingCubeBlockEntity cube = place(context, A, block);
                var menu = ((NamedScreenHandlerFactory) cube).createMenu(1, player.getInventory(), player);
                for (Item item : new Item[]{PhaseTeleportersMod.COLOR_CONFIGURATOR, PhaseTeleportersMod.PORTABLE_TELEPORT}) {
                    ItemStack tool = new ItemStack(item);
                    var rechargeable = (PEChargeableItem) item;
                    rechargeable.insertPE(tool, rechargeable.getCapacityPE(tool), false);
                    context.assertFalse(cube.isValid(1, tool), "Tools cannot feed the cube");
                    context.assertFalse(menu.getSlot(0).canInsert(tool), "Discharge GUI rejects tools");
                    context.assertTrue(menu.getSlot(1).canInsert(tool), "Charge GUI still accepts tools");
                    cube.setStack(1, tool);
                    context.assertTrue(cube.getStack(1).isEmpty(), "Direct discharge insertion also rejects tools");
                    player.getInventory().setStack(0, tool);
                    menu.quickMove(player, 29);
                    context.assertTrue(cube.getStack(0).isOf(item), "Shift-click routes tools to the charging slot");
                    context.assertTrue(cube.getStack(1).isEmpty(), "Shift-click never puts tools into discharge");
                    cube.setStack(0, ItemStack.EMPTY);
                }
                ItemStack battery = chargedBattery();
                context.assertTrue(cube.isValid(1, battery), "Battery may feed the cube");
                context.assertTrue(menu.getSlot(0).canInsert(battery), "Discharge GUI accepts batteries");
                menu.onClosed(player);
            }
            context.removeBlock(A);
            EnergyCubeBlockEntity cube = place(context, A, PhaseTeleportersMod.ENERGY_CUBE);
            cube.restoreStoredEnergy(1000);
            ItemStack legacyTool = new ItemStack(PhaseTeleportersMod.COLOR_CONFIGURATOR);
            configurator().insertPE(legacyTool, 200_000, false);
            var lookup = context.getWorld().getRegistryManager();
            var saved = cube.createNbt(lookup);
            var oldSlots = DefaultedList.ofSize(2, ItemStack.EMPTY);
            oldSlots.set(1, legacyTool);
            Inventories.writeNbt(saved, oldSlots, lookup);
            cube.read(saved, lookup);
            context.assertTrue(cube.getStack(1).isOf(PhaseTeleportersMod.COLOR_CONFIGURATOR),
                    "Already inserted tools survive loading for retrieval");
            EnergyCubeBlockEntity.tick(context.getWorld(), cube.getPos(), cube.getCachedState(), cube);
            context.assertEquals(configurator().getStoredPE(cube.getStack(1)), 200_000L, "Legacy tool is never drained");
            context.assertEquals(cube.getStored(), 1000L, "Legacy tool cannot supply the cube");
        } finally {
            disconnect(player);
        }
        context.complete();
    }
}
