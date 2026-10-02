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
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class TeleportationFrameMonitorBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<TeleportationFrameMonitorBlock> CODEC = createCodec(TeleportationFrameMonitorBlock::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public TeleportationFrameMonitorBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<TeleportationFrameMonitorBlock> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportationFrameMonitorBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (placer instanceof ServerPlayerEntity player
                && world.getBlockEntity(pos) instanceof TeleportationFrameMonitorBlockEntity monitor)
            TeleportationFrameMonitorNetworking.open(player, monitor);
    }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (hit.getSide() != state.get(FACING)) return ActionResult.PASS;
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof TeleportationFrameMonitorBlockEntity monitor)
            TeleportationFrameMonitorNetworking.open(serverPlayer, monitor);
        return ActionResult.SUCCESS;
    }
}
