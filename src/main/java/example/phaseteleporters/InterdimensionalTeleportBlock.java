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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class InterdimensionalTeleportBlock extends BlockWithEntity {
    public static final MapCodec<InterdimensionalTeleportBlock> CODEC = createCodec(InterdimensionalTeleportBlock::new);

    public InterdimensionalTeleportBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<InterdimensionalTeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new InterdimensionalTeleportBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override public void onPlaced(World world, BlockPos pos, BlockState state,
            net.minecraft.entity.LivingEntity placer, net.minecraft.item.ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof InterdimensionalTeleportBlockEntity teleport) {
            if (!PESecurity.canOpen(player, teleport)) return ActionResult.SUCCESS;
            teleport.refreshPortal();
            if (serverPlayer.openHandledScreen(teleport).isPresent()) {
                InterdimensionalTeleportNetworking.sendSnapshot(serverPlayer, teleport);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.INTERDIMENSIONAL_TELEPORT_BLOCK_ENTITY,
                InterdimensionalTeleportBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            InterdimensionalTeleportBlockEntity teleport = world.getBlockEntity(pos)
                    instanceof InterdimensionalTeleportBlockEntity found ? found : null;
            if (teleport != null) {
                teleport.releaseAnchor();
                ItemScatterer.spawn(world, pos, teleport);
                teleport.clearPortal();
            }
            InterdimensionalTeleportIndex.get(serverWorld).remove(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
