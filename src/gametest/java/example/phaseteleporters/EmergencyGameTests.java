package example.phaseteleporters;

import net.minecraft.item.*;
import net.minecraft.block.Blocks;
import net.minecraft.test.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class EmergencyGameTests extends PhaseGameTests {
    @GameTest(templateName = ROOM, tickLimit = 30)
    public void pendingRescueHonorsBindingAndPortableChanges(TestContext c) {
        EmergencyTeleportBlockEntity pad = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT);
        EmergencyTeleportBlockEntity other = place(c, B, PhaseTeleportersMod.EMERGENCY_TELEPORT);
        pad.restoreStoredEnergy(600_000); other.restoreStoredEnergy(200_000);
        var players = new java.util.ArrayList<net.minecraft.server.network.ServerPlayerEntity>();
        var origins = new java.util.ArrayList<Vec3d>();
        for (int change = 0; change < 4; change++) {
            var player = player(c); players.add(player); origins.add(player.getPos());
            player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
            var profile = EmergencyTeleportState.get(player.getServer()).profile(player.getUuid());
            profile.bind(pad);
            c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, player.getDamageSources().generic(), -10),
                    1f, "Rescue initially prepared");
            switch (change) {
                case 0 -> profile.enabled = false;
                case 1 -> profile.unbind();
                case 2 -> profile.bind(other);
                case 3 -> player.getInventory().setStack(2, ItemStack.EMPTY);
            }
        }
        c.assertEquals(pad.getStored(), 200_000L, "Four rescue charges reserved");
        c.runAtTick(4, () -> {
            try {
                c.assertEquals(pad.getStored(), 600_000L, "All cancelled rescue charges refunded");
                c.assertEquals(other.getStored(), 200_000L, "Rebinding does not charge the new pad");
                for (int i = 0; i < players.size(); i++)
                    c.assertTrue(players.get(i).getPos().squaredDistanceTo(origins.get(i)) < .01,
                            "Cancelled rescue leaves player in place: " + i);
            } finally { players.forEach(PhaseGameTests::disconnect); }
            c.complete();
        });
    }

    @GameTest(templateName = ROOM)
    public void bindingSelectsExactPadAndRejectsReplacement(TestContext c) {
        var player = player(c);
        try {
            player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
            EmergencyTeleportBlockEntity a = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT), b = place(c, B, PhaseTeleportersMod.EMERGENCY_TELEPORT);
            c.assertTrue(EmergencyTeleportRescue.bind(player, a), "First pad binding");
            c.assertTrue(EmergencyTeleportRescue.bind(player, b), "Rebinding selects second pad");
            var profile = EmergencyTeleportState.get(player.getServer()).boundProfile(player.getUuid());
            c.assertFalse(profile.matches(a), "First pad no longer selected"); c.assertTrue(profile.matches(b), "Second pad selected");
            c.removeBlock(B); EmergencyTeleportBlockEntity replacement = place(c, B, PhaseTeleportersMod.EMERGENCY_TELEPORT);
            c.assertFalse(profile.matches(replacement), "Replacement at same position cannot hijack binding");
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void portableTotemAbsorptionAndDimensionChecks(TestContext c) {
        var player = player(c);
        try {
            EmergencyTeleportBlockEntity pad = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT); pad.restoreStoredEnergy(200_000);
            var profile = EmergencyTeleportState.get(player.getServer()).profile(player.getUuid()); profile.bind(pad);
            var damage = player.getDamageSources().generic();
            c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, damage, -10), -10f, "No portable means no rescue");
            player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
            for (Hand hand : Hand.values()) {
                player.setStackInHand(hand, new ItemStack(Items.TOTEM_OF_UNDYING));
                c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, damage, -10), -10f, "Totem in either hand blocks rescue");
                player.setStackInHand(hand, ItemStack.EMPTY);
            }
            player.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_ABSORPTION).setBaseValue(4);
            player.setAbsorptionAmount(4);
            c.assertEquals(player.getAbsorptionAmount(), 4f, "Fixture has four absorption HP");
            c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, damage, 1), 1f, "Absorption counts towards threshold");
            player.setAbsorptionAmount(0); profile.dimension = World.NETHER;
            c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, damage, -10), -10f, "Different dimension blocks rescue");
            c.assertEquals(pad.getStored(), 200_000L, "Suppressed rescues do not spend energy");
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM)
    public void unboundPortableMenuDoesNotOpen(TestContext c) {
        var player = player(c);
        try {
            var portable = (PortableTeleportItem) PhaseTeleportersMod.PORTABLE_TELEPORT; var stack = new ItemStack(portable);
            portable.setMode(stack, PortableTeleportItem.Mode.EMERGENCY); player.setStackInHand(Hand.MAIN_HAND, stack);
            var before = player.currentScreenHandler; portable.use(c.getWorld(), player, Hand.MAIN_HAND);
            c.assertTrue(player.currentScreenHandler == before, "Unbound menu stays closed without crashing");
        } finally { disconnect(player); }
        c.complete();
    }

    @GameTest(templateName = ROOM, tickLimit = 30)
    public void failedArrivalRefundsReservedEnergy(TestContext c) {
        var player = player(c);
        EmergencyTeleportBlockEntity pad = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT);
        pad.restoreStoredEnergy(200_000);
        player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
        EmergencyTeleportState.get(player.getServer()).profile(player.getUuid()).bind(pad);
        Vec3d before = player.getPos();
        c.assertEquals(EmergencyTeleportRescue.beforeHealthDamage(player, player.getDamageSources().generic(), -10),
                1f, "Preparing rescue protects against the predicted lethal hit");
        c.assertEquals(pad.getStored(), 100_000L, "Preparation reserves one rescue charge");
        c.setBlockState(A.up(), Blocks.STONE);
        c.runAtTick(4, () -> {
            try {
                c.assertEquals(pad.getStored(), 200_000L, "Obstructed arrival refunds the whole charge");
                c.assertTrue(player.getPos().squaredDistanceTo(before) < .01, "Failed rescue does not move player");
            } finally { disconnect(player); }
            c.complete();
        });
    }

    @GameTest(templateName = ROOM, tickLimit = 30)
    public void fallPredictionHonorsHotbarWaterBucket(TestContext c) {
        var player = player(c);
        EmergencyTeleportBlockEntity pad = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT);
        pad.restoreStoredEnergy(200_000);
        player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
        player.getInventory().setStack(1, new ItemStack(Items.WATER_BUCKET));
        var profile = EmergencyTeleportState.get(player.getServer()).profile(player.getUuid());
        profile.bind(pad); profile.skipWaterBucket = true; player.fallDistance = 40;
        c.assertFalse(EmergencyTeleportRescue.beforeFallMovement(player, new Vec3d(0, -1, 0)), "Hotbar bucket suppresses fall rescue");
        c.assertEquals(pad.getStored(), 200_000L, "Suppression does not spend energy");
        profile.skipWaterBucket = false;
        c.assertTrue(EmergencyTeleportRescue.beforeFallMovement(player, new Vec3d(0, -1, 0)), "Imminent lethal landing triggers rescue");
        player.fallDistance = 0;
        c.runAtTick(4, () -> {
            try {
                c.assertEquals(pad.getStored(), 100_000L, "Fall rescue charged once");
                c.assertTrue(player.squaredDistanceTo(pad.getPos().getX() + .5, pad.getPos().getY() + EmergencyTeleportBlockEntity.HEIGHT,
                        pad.getPos().getZ() + .5) < .01, "Fall rescue reaches the linked pad");
            } finally { disconnect(player); }
            c.complete();
        });
    }

    @GameTest(templateName = ROOM, tickLimit = 120)
    public void actualLethalDamageRescuedThroughMixin(TestContext c) {
        var player = player(c);
        EmergencyTeleportBlockEntity pad = place(c, A, PhaseTeleportersMod.EMERGENCY_TELEPORT); pad.restoreStoredEnergy(200_000);
        player.getInventory().setStack(2, new ItemStack(PhaseTeleportersMod.PORTABLE_TELEPORT));
        EmergencyTeleportState.get(player.getServer()).profile(player.getUuid()).bind(pad);
        c.runAtTick(70, () -> {
            // Wait out vanilla spawn protection, then exercise the actual applyDamage mixin.
            player.damage(player.getDamageSources().outOfWorld(), 100);
            c.assertTrue(player.isAlive() && player.getHealth() > 0, "Lethal damage intercepted by rescue mixin");
        });
        c.runAtTick(74, () -> {
            try {
                c.assertTrue(player.squaredDistanceTo(pad.getPos().getX() + .5, pad.getPos().getY() + EmergencyTeleportBlockEntity.HEIGHT,
                        pad.getPos().getZ() + .5) < .01, "Player arrives at linked pad");
                c.assertEquals(pad.getStored(), 100_000L, "Exactly one rescue energy charge");
                c.assertEquals(player.fallDistance, 0f, "Fall distance reset");
            } finally { disconnect(player); }
            c.complete();
        });
    }
}
