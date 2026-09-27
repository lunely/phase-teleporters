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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
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

public final class PortalPlaneBlock extends BlockWithEntity {
    private static final long PE_PER_TELEPORT = 500;
    public static final MapCodec<PortalPlaneBlock> CODEC = createCodec(PortalPlaneBlock::new);
    public static final IntProperty COLOR = IntProperty.of("color", 0, 15);

    public PortalPlaneBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(Properties.HORIZONTAL_AXIS, Direction.Axis.X).with(COLOR, PortalColors.DEFAULT));
    }

    @Override protected MapCodec<PortalPlaneBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_AXIS, COLOR);
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PortalPlaneBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                  ShapeContext context) { return VoxelShapes.empty(); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportsMod.PORTAL_PLANE_BLOCK_ENTITY,
                PortalPlaneBlockEntity::tick);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerWorld serverWorld) || !(entity instanceof ServerPlayerEntity player)
                || PortalReentryGuard.isBlocked(player)
                || !PortalReentryGuard.intersectsPlane(player.getBoundingBox(), state, pos)
                || !(world.getBlockEntity(pos) instanceof PortalPlaneBlockEntity plane)) return;
        TeleportBlockEntity source = plane.sourceTeleport(serverWorld);
        if (source == null || source.getStored() < PE_PER_TELEPORT) return;
        TeleportBlockEntity destination = plane.findDestination(serverWorld);
        if (destination == null || destination.getWorld() != serverWorld
                || !destination.getWorld().getRegistryKey().equals(serverWorld.getRegistryKey())) return;
        BlockPos exit = destination.getPos().up();
        TeleportStructure.Bounds destinationBounds = destination.getStructureBounds().orElseThrow();
        Direction.Axis axis = destinationBounds.axis();
        // Put the player's center just in front of the plane while their hitbox still touches it.
        double x = exit.getX() + 0.5 + (axis == Direction.Axis.Z ? 0.18 : 0.0);
        double z = exit.getZ() + 0.5 + (axis == Direction.Axis.X ? 0.18 : 0.0);
        double sourceX = player.getX();
        double sourceY = player.getY();
        double sourceZ = player.getZ();
        PortalReentryGuard.mark(player, serverWorld, destinationBounds);
        if (player.teleport(serverWorld, x, exit.getY(), z,
                java.util.Set.of(), player.getYaw(), player.getPitch())) {
            source.extract(PE_PER_TELEPORT, false);
            playTeleportEffects(serverWorld, serverWorld, sourceX, sourceY, sourceZ,
                    x, exit.getY(), z);
        } else {
            PortalReentryGuard.clear(player);
        }
    }

    static void playTeleportEffects(ServerWorld sourceWorld, ServerWorld destinationWorld,
            double sourceX, double sourceY, double sourceZ,
            double destinationX, double destinationY, double destinationZ) {
        sourceWorld.spawnParticles(ParticleTypes.PORTAL, sourceX, sourceY + 0.9, sourceZ,
                18, 0.3, 0.6, 0.3, 0.08);
        destinationWorld.spawnParticles(ParticleTypes.PORTAL,
                destinationX, destinationY + 0.9, destinationZ,
                18, 0.3, 0.6, 0.3, 0.08);
        destinationWorld.playSound(null, destinationX, destinationY, destinationZ,
                destinationWorld.random.nextBoolean()
                        ? SoundEvents.ENTITY_ENDERMAN_TELEPORT
                        : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
