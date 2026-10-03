package example.phaseteleporters;

import com.mojang.authlib.GameProfile;
import example.phaseteleporters.energy.*;
import io.netty.channel.embedded.EmbeddedChannel;
import java.lang.reflect.Method;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.server.network.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;

/** Shared fixtures for real server GameTests, never included in the release jar. */
public abstract class PhaseGameTests implements FabricGameTest {
    protected static final String ROOM = "phaseteleporters_tests:room";
    protected static final BlockPos A = new BlockPos(3, 1, 3), B = new BlockPos(11, 1, 3);
    private static final Map<BlockEntity, Tank> TANKS = new WeakHashMap<>();
    private static boolean tanksRegistered;

    @Override public void invokeTestMethod(TestContext context, Method method) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
            context.setBlockState(x, 0, z, Blocks.STONE);
        FabricGameTest.super.invokeTestMethod(context, method);
    }
    protected static <T extends BlockEntity> T place(TestContext c, BlockPos pos, Block block) {
        c.setBlockState(pos, block);
        T entity = c.getBlockEntity(pos);
        c.assertTrue(entity != null, "Block entity must exist: " + block);
        return entity;
    }
    protected static void item(TestContext c, ItemStack stack, Item item, int count) {
        c.assertTrue(stack.isOf(item) && stack.getCount() == count, "Expected " + count + " " + item + ", got " + stack);
    }
    protected static BatteryItem batteryItem() { return (BatteryItem) PhaseTeleportersMod.BATTERY; }
    protected static ItemStack chargedBattery() {
        var stack = new ItemStack(PhaseTeleportersMod.BATTERY);
        batteryItem().insertPE(stack, BatteryItem.CAPACITY, false);
        return stack;
    }
    protected static String frequency() { return UUID.randomUUID().toString().substring(0, 24); }
    protected static QuantumTeleportBlockEntity quantum(TestContext c, BlockPos pos, String name) {
        QuantumTeleportBlockEntity entity = place(c, pos, PhaseTeleportersMod.QUANTUM_TELEPORT);
        var state = QuantumFrequencyState.get(c.getWorld());
        if (!state.contains(name, false, null)) state.create(name, false, null, "test");
        entity.setFrequency(name, false, null);
        for (Direction side : Direction.values()) entity.setSideMode(side, PESideMode.DISABLED);
        state.register(entity);
        return entity;
    }
    protected static void frame(ServerWorld world, BlockPos base, Block controller, Direction.Axis axis) {
        for (int offset = -1; offset <= 1; offset++) for (int height = 0; height <= 3; height++) {
            BlockPos pos = base.add(axis == Direction.Axis.X ? offset : 0, height, axis == Direction.Axis.Z ? offset : 0);
            Block block = height == 0 && offset == 0 ? controller
                    : height == 0 || height == 3 || offset != 0 ? PhaseTeleportersMod.TELEPORTATION_FRAME : Blocks.AIR;
            world.setBlockState(pos, block.getDefaultState(), 3);
        }
    }
    protected static ServerPlayerEntity player(TestContext c) {
        // Minecraft's built-in GameTest mock always overrides isCreative() to true.
        var data = ConnectedClientData.createDefault(new GameProfile(UUID.randomUUID(), "PhaseTest"), false);
        var player = new ServerPlayerEntity(c.getWorld().getServer(), c.getWorld(), data.gameProfile(), data.syncedOptions());
        var connection = new ClientConnection(NetworkSide.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.getServer().getPlayerManager().onPlayerConnect(connection, player, data);
        player.changeGameMode(GameMode.SURVIVAL);
        Vec3d pos = c.getAbsolute(new Vec3d(8.5, 1, 10.5));
        player.requestTeleport(pos.x, pos.y, pos.z);
        return player;
    }
    protected static void disconnect(ServerPlayerEntity player) {
        player.getServer().getPlayerManager().remove(player);
        player.discard();
        if (player.networkHandler != null) player.networkHandler.disconnect(net.minecraft.text.Text.literal("Test complete"));
    }
    protected static Tank tank(TestContext c, BlockPos pos) {
        if (!tanksRegistered) {
            FluidStorage.SIDED.registerForBlocks((world, at, state, entity, side) -> TANKS.get(entity), Blocks.BARREL);
            tanksRegistered = true;
        }
        BlockEntity entity = place(c, pos, Blocks.BARREL);
        Tank tank = new Tank(); TANKS.put(entity, tank); return tank;
    }
    protected static final class Tank extends SingleVariantStorage<FluidVariant> {
        @Override protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
        @Override protected long getCapacity(FluidVariant variant) { return FluidConstants.BUCKET * 4; }
    }
}
