package example.phaseteleports;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class EnergyCubeBlock extends BlockWithEntity {
    public static final MapCodec<EnergyCubeBlock> CODEC = createCodec(EnergyCubeBlock::new);

    public EnergyCubeBlock(Settings settings) { super(settings); }

    @Override protected MapCodec<EnergyCubeBlock> getCodec() { return CODEC; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCubeBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type,
                PhaseTeleportsMod.ENERGY_CUBE_BLOCK_ENTITY, EnergyCubeBlockEntity::tick);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube)
            player.openHandledScreen(cube);
        return ActionResult.SUCCESS;
    }

    @Override protected List<ItemStack> getDroppedStacks(
            BlockState state, LootContextParameterSet.Builder builder) {
        ItemStack drop = new ItemStack(PhaseTeleportsMod.ENERGY_CUBE_ITEM);
        if (builder.getOptional(LootContextParameters.BLOCK_ENTITY) instanceof EnergyCubeBlockEntity cube
                && cube.getStored() > 0) {
            NbtCompound data = new NbtCompound();
            data.putLong("PE", cube.getStored());
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, drop, data);
        }
        return List.of(drop);
    }

    @Override public void onPlaced(World world, BlockPos pos, BlockState state,
            LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient || !(world.getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube)) return;
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data != null) cube.insert(Math.max(0, data.copyNbt().getLong("PE")), false);
    }

    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos,
            BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube)
            ItemScatterer.spawn(world, pos, cube);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
