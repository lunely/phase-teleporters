package example.phaseteleports;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class CoalGeneratorBlock extends BlockWithEntity {
    public static final MapCodec<CoalGeneratorBlock> CODEC = createCodec(CoalGeneratorBlock::new);

    public CoalGeneratorBlock(Settings settings) { super(settings); }

    @Override protected MapCodec<CoalGeneratorBlock> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoalGeneratorBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type,
                PhaseTeleportsMod.COAL_GENERATOR_BLOCK_ENTITY, CoalGeneratorBlockEntity::tick);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CoalGeneratorBlockEntity generator)
            player.openHandledScreen(generator);
        return ActionResult.SUCCESS;
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof Inventory inventory)
            ItemScatterer.spawn(world, pos, inventory);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
