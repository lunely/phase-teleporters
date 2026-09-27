package example.phaseteleports;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.World;
import net.minecraft.world.BlockView;

public final class InterdimensionalPortalPlaneBlock extends BlockWithEntity {
    private static final long PE_PER_TELEPORT = 500;
    public static final MapCodec<InterdimensionalPortalPlaneBlock> CODEC = createCodec(InterdimensionalPortalPlaneBlock::new);
    public static final IntProperty COLOR = IntProperty.of("color", 0, 15);

    public InterdimensionalPortalPlaneBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(Properties.HORIZONTAL_AXIS, Direction.Axis.X).with(COLOR, InterdimensionalFrequencyState.DEFAULT_COLOR));
    }

    @Override protected MapCodec<InterdimensionalPortalPlaneBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_AXIS, COLOR);
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InterdimensionalPortalPlaneBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                  ShapeContext context) { return VoxelShapes.empty(); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportsMod.INTERDIMENSIONAL_PORTAL_PLANE_BLOCK_ENTITY,
                InterdimensionalPortalPlaneBlockEntity::tick);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerWorld serverWorld) || !(entity instanceof ServerPlayerEntity player)) return;
        boolean blocked = InterdimensionalReentryGuard.isBlocked(player);
        InterdimensionalReentryGuard.collision(player, serverWorld, pos, blocked);
        if (blocked || !PortalReentryGuard.intersectsPlane(player.getBoundingBox(), state, pos)
                || !(world.getBlockEntity(pos) instanceof InterdimensionalPortalPlaneBlockEntity plane)) return;
        InterdimensionalTeleportBlockEntity source = plane.sourceTeleport(serverWorld);
        if (source == null || source.getStored() < PE_PER_TELEPORT) return;
        InterdimensionalTeleportBlockEntity destination = plane.findDestination(serverWorld);
        if (destination == null || !(destination.getWorld() instanceof ServerWorld destinationWorld)) return;
        BlockPos exit = destination.getPos().up();
        InterdimensionalTeleportStructure.Bounds destinationBounds =
                destination.getStructureBounds().orElseThrow();
        Direction.Axis axis = destinationBounds.axis();
        // Put the player's center just in front of the plane while their hitbox still touches it.
        double x = exit.getX() + 0.5 + (axis == Direction.Axis.Z ? 0.18 : 0.0);
        double z = exit.getZ() + 0.5 + (axis == Direction.Axis.X ? 0.18 : 0.0);
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();
        InterdimensionalReentryGuard.mark(player, serverWorld, destinationWorld, destinationBounds);
        if (serverWorld != destinationWorld)
            ServerPlayNetworking.send(player, new InterdimensionalTerrainPayload(true));
        boolean teleported = player.teleport(destinationWorld, x, exit.getY(), z,
                java.util.Set.of(), player.getYaw(), player.getPitch());
        if (teleported && player.getWorld() == destinationWorld) {
            source.extract(PE_PER_TELEPORT, false);
            InterdimensionalReentryGuard.transferCompleted(player, serverWorld, destinationWorld);
            PortalPlaneBlock.playTeleportEffects(serverWorld, destinationWorld,
                    sourceX, sourceY, sourceZ, x, exit.getY(), z);
        } else {
            if (serverWorld != destinationWorld)
                ServerPlayNetworking.send(player, new InterdimensionalTerrainPayload(false));
            InterdimensionalReentryGuard.clear(player, "teleport-failed");
        }
    }
}
