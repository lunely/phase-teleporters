package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class EmergencyTeleportBlock extends StoredEnergyBlock {
    public static final MapCodec<EmergencyTeleportBlock> CODEC = createCodec(EmergencyTeleportBlock::new);
    public static final IntProperty COLOR = TeleportationFrameBlock.COLOR;
    public static final BooleanProperty NORTH = Properties.NORTH;
    public static final BooleanProperty SOUTH = Properties.SOUTH;
    public static final BooleanProperty EAST = Properties.EAST;
    public static final BooleanProperty WEST = Properties.WEST;
    private static final VoxelShape[] SHAPES = buildShapes();

    public EmergencyTeleportBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(COLOR, FrameColors.DEFAULT)
                .with(NORTH, false).with(SOUTH, false).with(EAST, false).with(WEST, false));
    }

    private static VoxelShape[] buildShapes() {
        VoxelShape base = VoxelShapes.union(createCuboidShape(0, 0, 0, 16, .5, 16),
                createCuboidShape(1, .5, 1, 15, 1, 15));
        VoxelShape[] ports = {
            VoxelShapes.union(createCuboidShape(7, .5, 0, 9, 5, .5), createCuboidShape(5, 5, 0, 11, 11, .5)),
            VoxelShapes.union(createCuboidShape(7, .5, 15.5, 9, 5, 16), createCuboidShape(5, 5, 15.5, 11, 11, 16)),
            VoxelShapes.union(createCuboidShape(15.5, .5, 7, 16, 5, 9), createCuboidShape(15.5, 5, 5, 16, 11, 11)),
            VoxelShapes.union(createCuboidShape(0, .5, 7, .5, 5, 9), createCuboidShape(0, 5, 5, .5, 11, 11))
        };
        VoxelShape[] shapes = new VoxelShape[16];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = base;
            for (int port = 0; port < ports.length; port++) {
                if ((mask & (1 << port)) != 0) shape = VoxelShapes.union(shape, ports[port]);
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(COLOR, NORTH, SOUTH, EAST, WEST);
    }

    public static BooleanProperty portProperty(Direction side) {
        return switch (side) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("Ports are horizontal");
        };
    }

    public static boolean acceptsCable(Direction side) { return side != Direction.UP; }

    private static boolean sideCable(BlockView world, BlockPos pos, Direction side) {
        return world.getBlockState(pos.offset(side)).getBlock() instanceof BasicEnergyCableBlock;
    }

    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = getDefaultState();
        for (Direction side : Direction.Type.HORIZONTAL) {
            state = state.with(portProperty(side), sideCable(context.getWorld(), context.getBlockPos(), side));
        }
        return state;
    }

    @Override protected BlockState getStateForNeighborUpdate(BlockState state, Direction side,
            BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        return side.getAxis().isHorizontal()
                ? state.with(portProperty(side), neighborState.getBlock() instanceof BasicEnergyCableBlock)
                : state;
    }

    static void refreshConnections(World world, BlockPos pos, BlockState state) {
        BlockState updated = state;
        for (Direction side : Direction.Type.HORIZONTAL) {
            updated = updated.with(portProperty(side), sideCable(world, pos, side));
        }
        if (updated != state) world.setBlockState(pos, updated, Block.NOTIFY_ALL);
    }

    @Override protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var drops = super.getDroppedStacks(state, builder);
        for (var drop : drops) {
            if (drop.isOf(asItem())) FrameColors.setItemColor(drop, state.get(COLOR));
        }
        return drops;
    }

    @Override public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        FrameColors.setItemColor(stack, state.get(COLOR));
        return stack;
    }
    @Override protected MapCodec<EmergencyTeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EmergencyTeleportBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        int mask = (state.get(NORTH) ? 1 : 0) | (state.get(SOUTH) ? 2 : 0)
                | (state.get(EAST) ? 4 : 0) | (state.get(WEST) ? 8 : 0);
        return SHAPES[mask];
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.EMERGENCY_TELEPORT_BLOCK_ENTITY,
                EmergencyTeleportBlockEntity::tick);
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState next, boolean moved) {
        if (!state.isOf(next.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof EmergencyTeleportBlockEntity pad) {
            pad.releaseAnchor();
            pad.scatterEnergyItem();
        }
        super.onStateReplaced(state, world, pos, next, moved);
    }
    @Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof EmergencyTeleportBlockEntity pad
                && PESecurity.canOpen(player, pad)) serverPlayer.openHandledScreen(pad);
        return ActionResult.SUCCESS;
    }
}
