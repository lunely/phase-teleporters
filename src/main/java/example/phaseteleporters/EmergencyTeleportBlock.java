package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class EmergencyTeleportBlock extends StoredEnergyBlock {
    public static final MapCodec<EmergencyTeleportBlock> CODEC = createCodec(EmergencyTeleportBlock::new);
    private static final VoxelShape SHAPE = createCuboidShape(0, 0, 0, 16, 1, 16);
    public EmergencyTeleportBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<EmergencyTeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EmergencyTeleportBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
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
