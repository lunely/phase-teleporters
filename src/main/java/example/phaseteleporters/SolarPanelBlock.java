package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

public final class SolarPanelBlock extends StoredEnergyBlock {
    public static final MapCodec<SolarPanelBlock> CODEC = createCodec(SolarPanelBlock::new);
    public SolarPanelBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }
    @Override protected MapCodec<SolarPanelBlock> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(Properties.HORIZONTAL_FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SolarPanelBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.SOLAR_PANEL_BLOCK_ENTITY, SolarPanelBlockEntity::tick);
    }
    @Override public void onPlaced(World world, BlockPos pos, BlockState state, net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof SolarPanelBlockEntity panel && PESecurity.canOpen(player, panel))
            player.openHandledScreen(panel);
        return ActionResult.SUCCESS;
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof net.minecraft.inventory.Inventory inventory)
            net.minecraft.util.ItemScatterer.spawn(world, pos, inventory);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
