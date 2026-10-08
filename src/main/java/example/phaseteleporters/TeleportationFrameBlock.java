package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.world.World;

/** Plain frame block with persistent palette selection; no block entity is needed. */
public final class TeleportationFrameBlock extends Block {
    public static final MapCodec<TeleportationFrameBlock> CODEC = createCodec(TeleportationFrameBlock::new);
    public static final IntProperty COLOR = IntProperty.of("color", 0, PortalColors.count() - 1);

    public TeleportationFrameBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(COLOR, FrameColors.DEFAULT));
    }

    @Override protected MapCodec<TeleportationFrameBlock> getCodec() { return CODEC; }

    @Override protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && !TeleportStructure.isFrame(newState))
            PortalFrameUpdates.frameRemoved(world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override protected List<ItemStack> getDroppedStacks(BlockState state,
            LootContextParameterSet.Builder builder) {
        List<ItemStack> drops = super.getDroppedStacks(state, builder);
        for (ItemStack drop : drops) {
            if (drop.getItem() instanceof BlockItem item
                    && item.getBlock() instanceof TeleportationFrameBlock) {
                FrameColors.setItemColor(drop, state.get(COLOR));
            }
        }
        return drops;
    }

    @Override public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        ItemStack stack = super.getPickStack(world, pos, state);
        FrameColors.setItemColor(stack, state.get(COLOR));
        return stack;
    }
}
