package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class QuantumTeleportBlock extends StoredEnergyBlock {
    public static final MapCodec<QuantumTeleportBlock> CODEC = createCodec(QuantumTeleportBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public QuantumTeleportBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    @Override protected MapCodec<QuantumTeleportBlock> getCodec() { return CODEC; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new QuantumTeleportBlockEntity(pos, state);
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        PESecurity.onPlaced(world, pos, placer);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof QuantumTeleportBlockEntity teleport
                && PESecurity.canOpen(player, teleport)
                && serverPlayer.openHandledScreen(teleport).isPresent())
            QuantumTeleportNetworking.sendSnapshot(serverPlayer, teleport);
        return ActionResult.SUCCESS;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,
            BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, PhaseTeleportersMod.QUANTUM_TELEPORT_BLOCK_ENTITY,
                QuantumTeleportBlockEntity::tick);
    }
    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !world.isClient
                && world.getBlockEntity(pos) instanceof QuantumTeleportBlockEntity teleport) {
            teleport.releaseAnchor();
            teleport.scatterEnergyItem();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
