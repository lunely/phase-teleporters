package example.phaseteleporters;

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
    public static final IntProperty COLOR = IntProperty.of("color", 0, PortalColors.count() - 1);

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
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.PORTAL_PLANE_BLOCK_ENTITY,
                PortalPlaneBlockEntity::tick);
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerWorld)
                || !PortalReentryGuard.intersectsPortalBlock(entity.getBoundingBox(), pos)
                || PortalCollisionTeleport.isQueued(entity)) return;
        BlockPos contact = pos.toImmutable();
        PortalCollisionTeleport.enqueue(entity, () -> {
            if (!entity.isRemoved() && entity.getWorld() == world
                    && world.getBlockState(contact).isOf(this))
                teleportOnCollision(world.getBlockState(contact), world, contact, entity);
        });
    }

    private void teleportOnCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerWorld serverWorld)
                || !PortalReentryGuard.intersectsPortalBlock(entity.getBoundingBox(), pos)
                || !(world.getBlockEntity(pos) instanceof PortalPlaneBlockEntity plane)) return;
        if (!(entity instanceof ServerPlayerEntity player) || entity.hasVehicle() || entity.hasPassengers()) {
            teleportGroup(serverWorld, plane, entity);
            return;
        }
        if (PortalReentryGuard.isBlocked(player)) return;
        TeleportBlockEntity source = plane.sourceTeleport(serverWorld);
        if (source == null || source.getStored() < PE_PER_TELEPORT
                || !source.canPlayerTeleport(player)) return;
        TeleportBlockEntity destination = plane.findDestination(serverWorld);
        if (destination == null || destination.getWorld() != serverWorld
                || !destination.getWorld().getRegistryKey().equals(serverWorld.getRegistryKey())
                || !destination.canPlayerTeleport(player)) return;
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

    private static void teleportGroup(ServerWorld world, PortalPlaneBlockEntity plane, Entity entity) {
        java.util.List<Entity> group = PortalEntityTransfer.group(entity);
        for (Entity member : group) {
            if (member instanceof ServerPlayerEntity player) {
                if (PortalReentryGuard.isBlocked(player)) return;
            } else if (EntityPortalReentryGuard.isBlocked(member)) return;
        }
        TeleportBlockEntity source = plane.sourceTeleport(world);
        if (source == null || source.getStored() < PE_PER_TELEPORT) return;
        TeleportBlockEntity destination = plane.findDestination(world);
        if (destination == null || destination.getWorld() != world) return;
        for (Entity member : group) {
            if (member instanceof ServerPlayerEntity player
                    && (!source.canPlayerTeleport(player) || !destination.canPlayerTeleport(player))) return;
        }
        TeleportStructure.Bounds bounds = destination.getStructureBounds().orElse(null);
        if (bounds == null) return;
        BlockPos exit = destination.getPos().up();
        Direction.Axis axis = bounds.axis();
        double offset = PortalEntityTransfer.arrivalOffset(group, bounds.width() - 2, bounds.height() - 2);
        double x = exit.getX() + 0.5 + (axis == Direction.Axis.Z ? offset : 0.0);
        double z = exit.getZ() + 0.5 + (axis == Direction.Axis.X ? offset : 0.0);
        double sourceX = entity.getX();
        double sourceY = entity.getY();
        double sourceZ = entity.getZ();
        for (Entity member : group) {
            if (member instanceof ServerPlayerEntity player) PortalReentryGuard.mark(player, world, bounds);
            else EntityPortalReentryGuard.mark(member, world, bounds.interiorMin(), bounds.interiorMax());
        }
        if (PortalEntityTransfer.teleport(group, world, x, exit.getY(), z)) {
            source.extract(PE_PER_TELEPORT, false);
            playTeleportEffects(world, world, sourceX, sourceY, sourceZ, x, exit.getY(), z);
        } else {
            for (Entity member : group) {
                if (member instanceof ServerPlayerEntity player) PortalReentryGuard.clear(player);
                else EntityPortalReentryGuard.clear(member);
            }
        }
    }

    static void playTeleportEffects(ServerWorld sourceWorld, ServerWorld destinationWorld,
            double sourceX, double sourceY, double sourceZ,
            double destinationX, double destinationY, double destinationZ) {
        playTeleportEffects(sourceWorld, destinationWorld, sourceX, sourceY, sourceZ,
                destinationX, destinationY, destinationZ, 27, false);
    }

    static void playPortableTeleportEffects(ServerWorld sourceWorld, ServerWorld destinationWorld,
            double sourceX, double sourceY, double sourceZ,
            double destinationX, double destinationY, double destinationZ) {
        playTeleportEffects(sourceWorld, destinationWorld, sourceX, sourceY, sourceZ,
                destinationX, destinationY, destinationZ, 18, true);
    }

    private static void playTeleportEffects(ServerWorld sourceWorld, ServerWorld destinationWorld,
            double sourceX, double sourceY, double sourceZ,
            double destinationX, double destinationY, double destinationZ,
            int particles, boolean departureSound) {
        sourceWorld.spawnParticles(ParticleTypes.PORTAL, sourceX, sourceY + 0.9, sourceZ,
                particles, 0.3, 0.6, 0.3, 0.08);
        destinationWorld.spawnParticles(ParticleTypes.PORTAL,
                destinationX, destinationY + 0.9, destinationZ,
                particles, 0.3, 0.6, 0.3, 0.08);
        var sound = destinationWorld.random.nextBoolean()
                ? SoundEvents.ENTITY_ENDERMAN_TELEPORT : SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT;
        if (departureSound) {
            sourceWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    sourceX, sourceY + 0.9, sourceZ, 12, 0.3, 0.6, 0.3, 0.08);
            sourceWorld.playSound(null, sourceX, sourceY, sourceZ,
                    sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        destinationWorld.playSound(null, destinationX, destinationY, destinationZ,
                sound,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
