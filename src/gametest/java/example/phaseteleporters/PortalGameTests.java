package example.phaseteleporters;

import example.phaseteleporters.energy.PERedstoneMode;
import java.util.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public final class PortalGameTests extends PhaseGameTests {
    @GameTest(templateName = ROOM)
    public void portableMenuRequiresTheHandThatOpenedIt(TestContext c) {
        var player = player(c);
        try {
            for (var hand : net.minecraft.util.Hand.values()) {
                player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
                player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
                var menu = new PortableTeleportScreenHandler(1, player.getInventory(), hand);
                c.assertTrue(menu.canUse(player), "Portable held in the selected hand");
                player.setStackInHand(hand, ItemStack.EMPTY);
                c.assertFalse(menu.canUse(player), "Other hand's portable cannot keep the old menu open");
            }
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void localFrameActivationAndBrokenFrameCleanup(TestContext c) {
        String frequency = frequency(); LocalFrequencyState.get(c.getWorld()).create(frequency, false, null, "test");
        frame(c.getWorld(), c.getAbsolutePos(A), PhaseTeleportersMod.TELEPORT, Direction.Axis.X);
        frame(c.getWorld(), c.getAbsolutePos(B), PhaseTeleportersMod.TELEPORT, Direction.Axis.Z);
        TeleportBlockEntity a = c.getBlockEntity(A), b = c.getBlockEntity(B);
        a.restoreStoredEnergy(1000); b.restoreStoredEnergy(1000);
        a.setFrequency(frequency, false, null); b.setFrequency(frequency, false, null); a.refreshPortal();
        c.assertTrue(a.isStructureValid() && b.isStructureValid(), "Both frame orientations recognized");
        c.expectBlock(PhaseTeleportersMod.PORTAL_PLANE, A.up());
        c.assertTrue(a.findDestinationFor(a.getPos().up()) == b, "Local portal finds matching partner");
        c.removeBlock(A.west().up(2)); a.refreshPortal();
        c.assertFalse(a.isStructureValid(), "Broken frame invalid");
        c.expectBlock(Blocks.AIR, A.up()); c.expectBlock(Blocks.AIR, A.up(2)); c.complete();
    }

    @GameTest(templateName = ROOM, tickLimit = 30)
    public void localCollisionTransfersOnceWithoutImmediateReturn(TestContext c) {
        String frequency = frequency(); LocalFrequencyState.get(c.getWorld()).create(frequency, false, null, "test");
        frame(c.getWorld(), c.getAbsolutePos(A), PhaseTeleportersMod.TELEPORT, Direction.Axis.X);
        frame(c.getWorld(), c.getAbsolutePos(B), PhaseTeleportersMod.TELEPORT, Direction.Axis.X);
        TeleportBlockEntity a = c.getBlockEntity(A), b = c.getBlockEntity(B);
        a.restoreStoredEnergy(1000); b.restoreStoredEnergy(1000);
        a.setFrequency(frequency, false, null); b.setFrequency(frequency, false, null); a.refreshPortal();
        var pig = c.spawnMob(EntityType.PIG, new Vec3d(A.getX() + .5, A.getY() + 1, A.getZ() + .5)); pig.setNoGravity(true);
        ((PortalPlaneBlock) PhaseTeleportersMod.PORTAL_PLANE).onEntityCollision(c.getBlockState(A.up()), c.getWorld(), a.getPos().up(), pig);
        c.runAtTick(4, () -> {
            try {
                c.assertTrue(pig.squaredDistanceTo(Vec3d.ofCenter(b.getPos().up())) < 2, "Collision reaches destination");
                c.assertEquals(a.getStored(), 500L, "Source charged exactly once");
                c.assertEquals(b.getStored(), 1000L, "No immediate return teleport");
            } finally { pig.discard(); }
            c.complete();
        });
    }

    @GameTest(templateName = ROOM, tickLimit = 200)
    public void interdimensionalPartnerAndRiderTransfer(TestContext c) {
        ServerWorld source = c.getWorld(), destination = source.getServer().getWorld(World.NETHER);
        c.assertTrue(destination != null, "Test world must include the Nether");
        BlockPos local = c.getAbsolutePos(A), remote = new BlockPos(local.getX(), 80, local.getZ());
        destination.getChunk(remote);
        // Arrival is in front of the frame and can cross a chunk boundary.
        destination.getChunk(remote.add(0, 1, 2));
        for (BlockPos pos : BlockPos.iterate(remote.add(-2, 0, -2), remote.add(2, 5, 2))) destination.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        frame(source, local, PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT, Direction.Axis.X);
        frame(destination, remote, PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT, Direction.Axis.X);
        var a = (InterdimensionalTeleportBlockEntity) source.getBlockEntity(local);
        var b = (InterdimensionalTeleportBlockEntity) destination.getBlockEntity(remote);
        String frequency = frequency(); InterdimensionalFrequencyState.get(source).create(frequency, false, null, "test");
        a.restoreStoredEnergy(1000); b.restoreStoredEnergy(1000);
        a.setFrequency(frequency, false, null); b.setFrequency(frequency, false, null); a.refreshPortal();
        var boat = c.spawnEntity(EntityType.BOAT, new Vec3d(3.5, 2, 9.5));
        var pig = c.spawnMob(EntityType.PIG, new Vec3d(3.5, 2, 9.5)); UUID boatId = boat.getUuid(), pigId = pig.getUuid();
        // This test checks transfer and riding, independently of falling and mob movement.
        boat.setNoGravity(true); pig.setNoGravity(true); pig.setAiDisabled(true);
        Runnable cleanup = () -> {
            for (UUID id : List.of(boatId, pigId)) {
                var entity = destination.getEntity(id); if (entity != null) entity.discard();
                entity = source.getEntity(id); if (entity != null) entity.discard();
            }
            b.clearPortal();
            for (BlockPos pos : BlockPos.iterate(remote.add(-1, 0, 0), remote.add(1, 3, 0))) destination.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        };
        try {
            c.assertTrue(a.findDestinationFor(local.up()) == b, "Find partner across dimensions");
            c.assertTrue(pig.startRiding(boat, true), "Set up passenger");
            c.assertTrue(PortalEntityTransfer.teleport(PortalEntityTransfer.group(pig), destination,
                    remote.getX() + .5, remote.getY() + 1, remote.getZ() + 2), "Cross-world group transfer");
            // Wait for the temporary portal ticket to activate entity tracking in the target chunk.
            c.addInstantFinalTask(() -> {
                var arrivedBoat = destination.getEntity(boatId); var arrivedPig = destination.getEntity(pigId);
                c.assertTrue(arrivedBoat != null && arrivedPig != null, "Both original UUIDs arrive");
                c.assertTrue(arrivedPig.getVehicle() == arrivedBoat, "Passenger attached to destination vehicle instance");
                c.assertTrue(source.getEntity(boatId) == null && source.getEntity(pigId) == null, "No source-world duplicates");
                cleanup.run();
            });
        } catch (RuntimeException | Error failure) {
            cleanup.run();
            throw failure;
        }
    }

    @GameTest(templateName = ROOM)
    public void removedPassengerRejectsTransferBeforeMovingVehicle(TestContext c) {
        var boat = c.spawnEntity(EntityType.BOAT, new Vec3d(3.5, 2, 9.5)); var pig = c.spawnMob(EntityType.PIG, new Vec3d(3.5, 2, 9.5));
        pig.startRiding(boat, true); var group = PortalEntityTransfer.group(pig); Vec3d before = boat.getPos(); pig.discard();
        try {
            c.assertFalse(PortalEntityTransfer.teleport(group, c.getWorld(), 100, 80, 100), "Removed passenger aborts transfer");
            c.assertEquals(boat.getPos(), before, "Vehicle remains at source");
        } finally { boat.discard(); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void portableSelectionAndRedstoneRestrictions(TestContext c) {
        var player = player(c);
        try {
            var portable = (PortableTeleportItem) PhaseTeleportersMod.PORTABLE_TELEPORT; var stack = new ItemStack(portable);
            portable.insertPE(stack, 10_000, false); portable.setSelection(stack, new PortableTeleportItem.Selection(frequency(), false, false));
            c.assertEquals(PortableTeleportNetworking.quickTeleport(player, stack, portable),
                    "message.phaseteleporters.portable.no_frequency", "Missing frequency returns a message");
            c.assertTrue(portable.getSelection(stack).name().isEmpty(), "Invalid selection cleared");
            String frequency = frequency(); LocalFrequencyState.get(c.getWorld()).create(frequency, false, null, "test");
            TeleportBlockEntity target = place(c, B, PhaseTeleportersMod.TELEPORT); target.restoreStoredEnergy(1000);
            target.setFrequency(frequency, false, null); target.setRedstoneMode(PERedstoneMode.WITH_SIGNAL);
            portable.setSelection(stack, new PortableTeleportItem.Selection(frequency, false, false)); Vec3d before = player.getPos();
            c.assertTrue(PortableTeleportNetworking.quickTeleport(player, stack, portable) != null, "Redstone-disabled target rejected");
            c.assertEquals(player.getPos(), before, "Rejected transfer does not move player");
            c.assertEquals(portable.getStoredPE(stack), 10_000L, "Rejected transfer does not spend energy");
            target.setRedstoneMode(PERedstoneMode.IGNORED);
            c.assertTrue(PortableTeleportNetworking.quickTeleport(player, stack, portable) == null, "Enabled target works");
            c.assertEquals(portable.getStoredPE(stack), 8_000L, "Success charges portable exactly once");
        } finally { disconnect(player); }
        c.complete();
    }
}
