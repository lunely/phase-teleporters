package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CreativeEnergyCubeBlock extends BlockWithEntity {
    public static final MapCodec<CreativeEnergyCubeBlock> CODEC = createCodec(CreativeEnergyCubeBlock::new);

    public CreativeEnergyCubeBlock(Settings settings) { super(settings); }

    @Override protected MapCodec<CreativeEnergyCubeBlock> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeEnergyCubeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type,
                PhaseTeleportersMod.CREATIVE_ENERGY_CUBE_BLOCK_ENTITY, CreativeEnergyCubeBlockEntity::tick);
    }

    @Override public void onPlaced(World world, BlockPos pos, BlockState state,
            net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
            BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CreativeEnergyCubeBlockEntity cube
                && PESecurity.canOpen(player, cube))
            player.openHandledScreen(cube);
        return ActionResult.SUCCESS;
    }

    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())
                && world.getBlockEntity(pos) instanceof CreativeEnergyCubeBlockEntity cube)
            ItemScatterer.spawn(world, pos, cube);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
