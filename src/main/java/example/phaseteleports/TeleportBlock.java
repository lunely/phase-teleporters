package example.phaseteleports;

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

public final class TeleportBlock extends BlockWithEntity {
    public static final MapCodec<TeleportBlock> CODEC = createCodec(TeleportBlock::new);

    public TeleportBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<TeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport) {
            teleport.refreshPortal();
            if (serverPlayer.openHandledScreen(teleport).isPresent()) {
                TeleportNetworking.sendSnapshot(serverPlayer, teleport);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportsMod.TELEPORT_BLOCK_ENTITY,
                TeleportBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            TeleportBlockEntity teleport = world.getBlockEntity(pos) instanceof TeleportBlockEntity found ? found : null;
            if (teleport != null) {
                teleport.releaseAnchor();
                ItemScatterer.spawn(world, pos, teleport);
                teleport.clearPortal();
            }
            LocalTeleportIndex.get(serverWorld).remove(pos);
            if (teleport != null) UnusedFrequencyCleanup.local(serverWorld, teleport.getFrequency(),
                    teleport.isPrivateFrequency(), teleport.getFrequencyOwner());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
