package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import example.phaseteleporters.energy.PEStorage;
import example.phaseteleporters.energy.EnergyApiCompat;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.World;
import example.phaseteleporters.energy.EnergyCableNetwork;

public final class BasicEnergyCableBlock extends BlockWithEntity {
    public static final MapCodec<BasicEnergyCableBlock> CODEC = createCodec(BasicEnergyCableBlock::new);
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    public static final BooleanProperty UP = Properties.UP;
    public static final BooleanProperty DOWN = Properties.DOWN;

    public BasicEnergyCableBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false).with(UP, false).with(DOWN, false));
    }

    @Override protected MapCodec<BasicEnergyCableBlock> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BasicEnergyCableBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type,
                PhaseTeleportersMod.BASIC_ENERGY_CABLE_BLOCK_ENTITY, BasicEnergyCableBlockEntity::tick);
    }

    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!world.isClient && !state.isOf(newState.getBlock())
                && world.getBlockEntity(pos) instanceof BasicEnergyCableBlockEntity cable)
            EnergyCableNetwork.onCableRemoved(world, pos, cable);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        BlockState state = getDefaultState();
        for (Direction direction : Direction.values())
            state = state.with(property(direction), connects(context.getWorld(), pos.offset(direction),
                    direction.getOpposite()));
        return state;
    }

    @Override protected BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
            BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return state.with(property(direction), connects(world, neighborPos, direction.getOpposite()));
    }

    static void refreshConnections(World world, BlockPos pos, BlockState state) {
        BlockState updated = state;
        for (Direction direction : Direction.values())
            updated = updated.with(property(direction), connects(world, pos.offset(direction),
                    direction.getOpposite()));
        if (updated != state) world.setBlockState(pos, updated, Block.NOTIFY_ALL);
    }

    private static boolean connects(BlockView world, BlockPos pos, Direction side) {
        Block block = world.getBlockState(pos).getBlock();
        if (block instanceof EmergencyTeleportBlock) return EmergencyTeleportBlock.acceptsCable(side);
        return block instanceof BasicEnergyCableBlock
                || block == PhaseTeleportersMod.CREATIVE_ENERGY_CUBE
                || block == PhaseTeleportersMod.ENERGY_CUBE
                || block == PhaseTeleportersMod.COAL_GENERATOR
                || block == PhaseTeleportersMod.INFUSION_STATION
                || block == PhaseTeleportersMod.CRUSHER
                || block == PhaseTeleportersMod.ELECTRIC_FURNACE
                || block == PhaseTeleportersMod.ENRICHMENT_CHAMBER
                || block == PhaseTeleportersMod.TELEPORT
                || block == PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT
                || world.getBlockEntity(pos) instanceof PEStorage
                || world instanceof World level && EnergyApiCompat.connects(level, pos, side);
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
            ShapeContext context) {
        VoxelShape shape = Block.createCuboidShape(5.75, 5.75, 5.75, 10.25, 10.25, 10.25);
        if (state.get(NORTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5.75, 5.75, 0, 10.25, 10.25, 5.75));
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5.75, 5.75, 10.25, 10.25, 10.25, 16));
        if (state.get(EAST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(10.25, 5.75, 5.75, 16, 10.25, 10.25));
        if (state.get(WEST)) shape = VoxelShapes.union(shape, Block.createCuboidShape(0, 5.75, 5.75, 5.75, 10.25, 10.25));
        if (state.get(UP)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5.75, 10.25, 5.75, 10.25, 16, 10.25));
        if (state.get(DOWN)) shape = VoxelShapes.union(shape, Block.createCuboidShape(5.75, 0, 5.75, 10.25, 5.75, 10.25));
        return shape;
    }
}
