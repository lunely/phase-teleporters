package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class EnrichmentChamberBlock extends BlockWithEntity {
    public static final MapCodec<EnrichmentChamberBlock> CODEC = createCodec(EnrichmentChamberBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    public EnrichmentChamberBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
    }

    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override protected MapCodec<EnrichmentChamberBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnrichmentChamberBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override public void onPlaced(World world, BlockPos pos, BlockState state,
            net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof EnrichmentChamberBlockEntity chamber
                && PESecurity.canOpen(player, chamber))
            player.openHandledScreen(chamber);
        return ActionResult.SUCCESS;
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
            BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type,
                PhaseTeleportersMod.ENRICHMENT_CHAMBER_BLOCK_ENTITY, EnrichmentChamberBlockEntity::tick);
    }

    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof Inventory inventory)
            ItemScatterer.spawn(world, pos, inventory);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
