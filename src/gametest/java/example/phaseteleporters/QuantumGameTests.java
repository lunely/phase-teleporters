package example.phaseteleporters;

import example.phaseteleporters.energy.*;
import java.util.UUID;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.item.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import team.reborn.energy.api.EnergyStorage;

public final class QuantumGameTests extends PhaseGameTests {
    @GameTest(templateName = ROOM)
    public void cachedRemoteViewsRejectDetachedAndReplacedStores(TestContext c) {
        String frequency = frequency(); var output = quantum(c, A, frequency); var input = quantum(c, B, frequency);
        output.setSideMode(Direction.WEST, PESideMode.ITEM_OUTPUT);
        output.setSideMode(Direction.NORTH, PESideMode.FLUID_OUTPUT);
        input.setSideMode(Direction.EAST, PESideMode.ITEM_INPUT);
        input.setSideMode(Direction.NORTH, PESideMode.FLUID_INPUT);
        ChestBlockEntity chest = place(c, B.east(), Blocks.CHEST);
        chest.setStack(0, new ItemStack(Items.DIAMOND, 8));
        var tank = tank(c, B.north()); tank.variant = FluidVariant.of(Fluids.WATER); tank.amount = FluidConstants.BUCKET;
        var items = ItemStorage.SIDED.find(c.getWorld(), output.getPos(), Direction.WEST);
        var fluids = FluidStorage.SIDED.find(c.getWorld(), output.getPos(), Direction.NORTH);
        var itemView = items.iterator().next(); var fluidView = fluids.iterator().next();
        c.assertEquals(itemView.getAmount(), 8L, "Cached item view starts populated");
        c.assertEquals(fluidView.getAmount(), FluidConstants.BUCKET, "Cached fluid view starts populated");
        input.removeFrequency();
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(itemView.extract(ItemVariant.of(Items.DIAMOND), 1, tx), 0L, "Detached item view rejects extraction");
            c.assertEquals(fluidView.extract(FluidVariant.of(Fluids.WATER), 1, tx), 0L, "Detached fluid view rejects extraction");
            tx.commit();
        }
        input.setFrequency(frequency, false, null);
        c.removeBlock(B.east()); place(c, B.east(), Blocks.CHEST);
        c.removeBlock(B.north()); var replacementTank = tank(c, B.north());
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(itemView.extract(ItemVariant.of(Items.DIAMOND), 1, tx), 0L, "Replaced chest invalidates cached view");
            c.assertEquals(fluidView.extract(FluidVariant.of(Fluids.WATER), 1, tx), 0L, "Replaced tank invalidates cached view");
            tx.commit();
        }
        c.assertEquals(tank.amount, FluidConstants.BUCKET, "Stale fluid store is not mutated");
        c.assertEquals(replacementTank.amount, 0L, "New fluid store stays empty");
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void withoutFrequencyAcceptsNothing(TestContext c) {
        QuantumTeleportBlockEntity quantum = place(c, A, PhaseTeleportersMod.QUANTUM_TELEPORT);
        c.assertEquals(quantum.insert(1000, false), 0L, "No-frequency energy input");
        c.assertEquals(quantum.getStored(), 0L, "No-frequency buffer stays zero");
        try (var tx = Transaction.openOuter()) {
            var items = ItemStorage.SIDED.find(c.getWorld(), quantum.getPos(), Direction.WEST);
            var fluid = FluidStorage.SIDED.find(c.getWorld(), quantum.getPos(), Direction.WEST);
            c.assertEquals(items.insert(ItemVariant.of(Items.DIAMOND), 1, tx), 0L, "No-frequency cargo input");
            c.assertEquals(fluid.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, tx), 0L, "No-frequency fluid input");
            c.assertTrue(fluid.iterator().hasNext(), "Empty port offers a safe blank view to pipes");
            c.assertEquals(fluid.iterator().next().getAmount(), 0L, "Blank view has no fluid");
        }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void sharedEnergyAndPrivateIsolation(TestContext c) {
        String frequency = frequency(); var first = quantum(c, A, frequency); var second = quantum(c, B, frequency);
        var third = quantum(c, new BlockPos(3, 1, 10), frequency()); UUID owner = UUID.randomUUID();
        QuantumFrequencyState.get(c.getWorld()).create(frequency, true, owner, "test"); third.setFrequency(frequency, true, owner);
        c.assertEquals(first.insert(7_000, false), 7_000L, "Energy accepted");
        c.assertEquals(second.getStored(), 7_000L, "Other endpoint sees shared energy");
        c.assertEquals(third.getStored(), 0L, "Private frequency has a separate buffer");
        c.assertEquals(first.transferTo(second, 2_000), 0L, "Same-frequency transfer cannot create energy");
        c.assertEquals(second.extract(2_000, false), 2_000L, "Shared extraction");
        c.assertEquals(first.getStored(), 5_000L, "Extraction visible at both endpoints"); first.removeFrequency();
        c.assertEquals(first.getStored(), 0L, "Detached endpoint exposes zero");
        c.assertEquals(second.getStored(), 5_000L, "Detaching preserves frequency energy"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void energyAbortRestoresSharedBufferAndInputQuotas(TestContext c) {
        String frequency = frequency(); var first = quantum(c, A, frequency); var second = quantum(c, B, frequency);
        first.setSideMode(Direction.WEST, PESideMode.INPUT); second.setSideMode(Direction.WEST, PESideMode.INPUT);
        EnergyStorage a = EnergyStorage.SIDED.find(c.getWorld(), first.getPos(), Direction.WEST);
        EnergyStorage b = EnergyStorage.SIDED.find(c.getWorld(), second.getPos(), Direction.WEST);
        c.assertTrue(a != null && b != null, "Team Reborn providers registered");
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(a.insert(1_000, tx), 1_000L, "First insertion");
            try (var nested = tx.openNested()) { c.assertEquals(b.insert(1_000, nested), 1_000L, "Nested insertion"); nested.commit(); }
            c.assertEquals(first.getStored(), 12_000L, "One API unit equals six joules");
        }
        c.assertEquals(first.getStored(), 0L, "Outer abort restores the shared buffer");
        c.assertEquals(first.insert(10_000, false), 10_000L, "Abort restores input quota");
        c.assertEquals(first.insert(1, false), 0L, "Input limit applies across all calls in one tick"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void frequencyAndAnchorTravelInBlockData(TestContext c) {
        String frequency = frequency(); var first = quantum(c, A, frequency); first.insert(4_321, false);
        first.setStack(0, new ItemStack(PhaseTeleportersMod.ANCHOR_UPGRADE)); NbtCompound data = new NbtCompound();
        first.writeStoredFrequency(data); first.writeStoredAnchorUpgrade(data); c.removeBlock(A);
        QuantumTeleportBlockEntity replacement = place(c, A, PhaseTeleportersMod.QUANTUM_TELEPORT);
        replacement.readStoredFrequency(data); replacement.readStoredAnchorUpgrade(data);
        c.assertEquals(replacement.getFrequency(), frequency, "Frequency survives replacement from item data");
        c.assertEquals(replacement.getStored(), 4_321L, "Shared energy retained");
        c.assertTrue(replacement.hasAnchorUpgrade(), "Chunk loader retained"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void chestPullRequiresRemoteOutputAndNeverBuffersCargo(TestContext c) {
        String frequency = frequency(); var input = quantum(c, A, frequency); input.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
        ChestBlockEntity source = place(c, A.west(), Blocks.CHEST); source.setStack(0, new ItemStack(Items.DIAMOND, 12));
        QuantumItemTransfer.tick(input); item(c, source.getStack(0), Items.DIAMOND, 12);
        var output = quantum(c, B, frequency); output.setSideMode(Direction.EAST, PESideMode.ITEM_OUTPUT);
        ChestBlockEntity target = place(c, B.east(), Blocks.CHEST); QuantumItemTransfer.tick(input);
        c.assertTrue(source.isEmpty(), "Chest drained once a remote output exists"); item(c, target.getStack(0), Items.DIAMOND, 12);
        c.assertTrue(input.isEmpty() && output.isEmpty(), "Teleporters do not buffer cargo"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void cargoAbortFullTargetAndDisabledSide(TestContext c) {
        String frequency = frequency(); var input = quantum(c, A, frequency); input.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
        var output = quantum(c, B, frequency); output.setSideMode(Direction.EAST, PESideMode.ITEM_OUTPUT);
        ChestBlockEntity chest = place(c, B.east(), Blocks.CHEST);
        var port = ItemStorage.SIDED.find(c.getWorld(), input.getPos(), Direction.WEST);
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(ItemVariant.of(Items.DIAMOND), 12, tx), 12L, "Simulation reaches remote chest"); }
        c.assertTrue(chest.isEmpty(), "Aborted insertion leaves remote chest unchanged");
        for (int slot = 0; slot < chest.size(); slot++) chest.setStack(slot, new ItemStack(Items.DIRT, 64));
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(ItemVariant.of(Items.DIAMOND), 12, tx), 0L, "Full target rejects cargo"); tx.commit(); }
        chest.clear(); output.setSideMode(Direction.EAST, PESideMode.DISABLED);
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(ItemVariant.of(Items.DIAMOND), 12, tx), 0L, "Disabled output rejects cargo"); tx.commit(); }
        c.assertTrue(chest.isEmpty(), "No hidden buffer"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void remoteMachineRejectsUnsupportedCargo(TestContext c) {
        String frequency = frequency(); var input = quantum(c, A, frequency); input.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
        var output = quantum(c, B, frequency); output.setSideMode(Direction.EAST, PESideMode.ITEM_OUTPUT);
        CrusherBlockEntity crusher = place(c, B.east(), PhaseTeleportersMod.CRUSHER); crusher.setSideMode(Direction.WEST, PESideMode.ITEM_INPUT);
        var port = ItemStorage.SIDED.find(c.getWorld(), input.getPos(), Direction.WEST);
        try (var tx = Transaction.openOuter()) {
            c.assertEquals(port.insert(ItemVariant.of(Items.DIAMOND), 12, tx), 0L, "Unsupported input rejected remotely");
            c.assertEquals(port.insert(ItemVariant.of(Items.OBSIDIAN), 2, tx), 2L, "Supported input accepted"); tx.commit();
        }
        item(c, crusher.getStack(0), Items.OBSIDIAN, 2); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void fluidAbortCommitAndVariantIsolation(TestContext c) {
        String frequency = frequency(); var input = quantum(c, A, frequency); input.setSideMode(Direction.WEST, PESideMode.FLUID_INPUT);
        var output = quantum(c, B, frequency); output.setSideMode(Direction.EAST, PESideMode.FLUID_OUTPUT); Tank tank = tank(c, B.east());
        var port = FluidStorage.SIDED.find(c.getWorld(), input.getPos(), Direction.WEST); var water = FluidVariant.of(Fluids.WATER);
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(water, FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "Fluid simulation"); }
        c.assertEquals(tank.amount, 0L, "Abort restores remote tank");
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(water, FluidConstants.BUCKET, tx), FluidConstants.BUCKET, "Fluid insertion"); tx.commit(); }
        c.assertEquals(tank.amount, FluidConstants.BUCKET, "Committed fluid reaches tank");
        try (var tx = Transaction.openOuter()) { c.assertEquals(port.insert(FluidVariant.of(Fluids.LAVA), FluidConstants.BUCKET, tx), 0L, "Fluids cannot mix"); tx.commit(); }
        c.assertEquals(tank.variant, water, "Tank keeps original variant"); c.complete();
    }

    @GameTest(templateName = ROOM)
    public void fluidPullConservesAmountAndStopsWithoutOutput(TestContext c) {
        String frequency = frequency(); var input = quantum(c, A, frequency); input.setSideMode(Direction.WEST, PESideMode.FLUID_INPUT);
        Tank source = tank(c, A.west()); source.variant = FluidVariant.of(Fluids.LAVA); source.amount = FluidConstants.BUCKET * 2;
        QuantumFluidTransfer.tick(input); c.assertEquals(source.amount, FluidConstants.BUCKET * 2, "No output means no extraction");
        var output = quantum(c, B, frequency); output.setSideMode(Direction.EAST, PESideMode.FLUID_OUTPUT); Tank target = tank(c, B.east());
        QuantumFluidTransfer.tick(input);
        c.assertEquals(source.amount, FluidConstants.BUCKET, "Pull limited to one bucket per input side");
        c.assertEquals(target.amount, FluidConstants.BUCKET, "Exactly the extracted amount arrives");
        c.assertEquals(target.variant, source.variant, "Non-water variant preserved");
        output.setRedstoneMode(PERedstoneMode.WITH_SIGNAL); QuantumFluidTransfer.tick(input);
        c.assertEquals(source.amount + target.amount, FluidConstants.BUCKET * 2, "Blocked route conserves fluid");
        c.assertEquals(target.amount, FluidConstants.BUCKET, "Redstone-disabled endpoint stops transfer"); c.complete();
    }
}
