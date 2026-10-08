package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.block.ShapeContext;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import java.util.List;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class TeleportationFrameMonitorBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<TeleportationFrameMonitorBlock> CODEC = createCodec(TeleportationFrameMonitorBlock::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty COLOR = TeleportationFrameBlock.COLOR;
    public static final int LIGHTS_OFF = PortalColors.count();
    public static final IntProperty MONITOR_COLOR = IntProperty.of("monitor_color", 0, LIGHTS_OFF);
    public TeleportationFrameMonitorBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH)
                .with(COLOR, FrameColors.DEFAULT).with(MONITOR_COLOR, FrameColors.DEFAULT));
    }
    @Override protected MapCodec<TeleportationFrameMonitorBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING, COLOR, MONITOR_COLOR);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportationFrameMonitorBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !TeleportStructure.isFrame(newState))
            PortalFrameUpdates.frameRemoved(world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
    public static int nextMonitorColor(int color) {
        if (color == LIGHTS_OFF) return PortalColors.displayColor(0);
        if (color == PortalColors.displayColor(PortalColors.count()-1)) return LIGHTS_OFF;
        return PortalColors.nextDisplayColor(color);
    }
    public static int itemMonitorColor(ItemStack stack) {
        var component = stack.get(DataComponentTypes.BLOCK_STATE);
        Integer value = component == null ? null : component.getValue(MONITOR_COLOR);
        return value != null && value >= 0 && value <= LIGHTS_OFF ? value : FrameColors.DEFAULT;
    }
    private static void preserveColors(ItemStack stack, BlockState state) {
        FrameColors.setItemColor(stack, state.get(COLOR));
        if (state.get(MONITOR_COLOR) != FrameColors.DEFAULT) {
            stack.set(DataComponentTypes.BLOCK_STATE, stack.getOrDefault(
                    DataComponentTypes.BLOCK_STATE, BlockStateComponent.DEFAULT)
                    .with(MONITOR_COLOR, state.get(MONITOR_COLOR)));
        }
    }
    @Override protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        var drops = super.getDroppedStacks(state, builder);
        for (var stack : drops) if (stack.isOf(PhaseTeleportersMod.TELEPORTATION_FRAME_MONITOR_ITEM))
            preserveColors(stack, state);
        return drops;
    }
    @Override public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        preserveColors(stack, state);
        return stack;
    }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape monitor = switch (state.get(FACING)) {
            case EAST -> createCuboidShape(16, 2, 0, 16.75, 14, 16);
            case SOUTH -> createCuboidShape(0, 2, 16, 16, 14, 16.75);
            case WEST -> createCuboidShape(-.75, 2, 0, 0, 14, 16);
            default -> createCuboidShape(0, 2, -.75, 16, 14, 0);
        };
        return VoxelShapes.union(VoxelShapes.fullCube(), monitor);
    }
    @Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (placer instanceof ServerPlayerEntity player
                && world.getBlockEntity(pos) instanceof TeleportationFrameMonitorBlockEntity monitor)
            TeleportationFrameMonitorNetworking.open(player, monitor);
    }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().getItem() instanceof ColorConfiguratorItem
                || player.getOffHandStack().getItem() instanceof ColorConfiguratorItem) return ActionResult.PASS;
        if (hit.getSide() != state.get(FACING)) return ActionResult.PASS;
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof TeleportationFrameMonitorBlockEntity monitor)
            TeleportationFrameMonitorNetworking.open(serverPlayer, monitor);
        return ActionResult.SUCCESS;
    }
}
