package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public final class TeleportBlock extends StoredEnergyBlock {
    public static final MapCodec<TeleportBlock> CODEC = createCodec(TeleportBlock::new);
    public static final IntProperty COLOR = TeleportationFrameBlock.COLOR;

    public TeleportBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(COLOR, FrameColors.DEFAULT));
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(COLOR);
    }

    @Override protected List<ItemStack> getDroppedStacks(BlockState state,
            LootContextParameterSet.Builder builder) {
        var drops = super.getDroppedStacks(state, builder);
        for (var drop : drops) {
            if (drop.isOf(asItem())) FrameColors.setItemColor(drop, state.get(COLOR));
        }
        return drops;
    }

    @Override public ItemStack getPickStack(WorldView world,
            BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        FrameColors.setItemColor(stack, state.get(COLOR));
        return stack;
    }
    @Override protected MapCodec<TeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TeleportBlockEntity(pos, state);
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
                && world.getBlockEntity(pos) instanceof TeleportBlockEntity teleport) {
            if (!PESecurity.canOpen(player, teleport)) return ActionResult.SUCCESS;
            teleport.refreshPortal();
            if (serverPlayer.openHandledScreen(teleport).isPresent()) {
                TeleportNetworking.sendSnapshot(serverPlayer, teleport);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.TELEPORT_BLOCK_ENTITY,
                TeleportBlockEntity::tick);
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world instanceof net.minecraft.server.world.ServerWorld serverWorld) {
            TeleportBlockEntity teleport = world.getBlockEntity(pos) instanceof TeleportBlockEntity found ? found : null;
            if (teleport != null) {
                teleport.releaseAnchor();
                teleport.scatterEnergyItem();
                teleport.clearPortal();
            }
            LocalTeleportIndex.get(serverWorld).remove(pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
