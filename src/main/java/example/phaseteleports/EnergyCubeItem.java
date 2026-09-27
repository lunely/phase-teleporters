package example.phaseteleports;

import example.phaseteleports.energy.PEBlockEntity;
import example.phaseteleports.energy.StoredPEItem;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

public final class EnergyCubeItem extends BlockItem {
    public EnergyCubeItem(Block block, Settings settings) {
        super(block, settings);
    }

    public static long getStoredPE(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        return component == null ? 0 : Math.clamp(component.copyNbt().getLong("PE"), 0L, PEBlockEntity.DEFAULT_CAPACITY);
    }

    public static void setStoredPE(ItemStack stack, long amount) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound data = component == null ? new NbtCompound() : component.copyNbt();
        data.putLong("PE", Math.clamp(amount, 0L, PEBlockEntity.DEFAULT_CAPACITY));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data);
    }

    @Override public boolean isItemBarVisible(ItemStack stack) { return true; }

    @Override public int getItemBarStep(ItemStack stack) {
        return StoredPEItem.itemBarStep(getStoredPE(stack), PEBlockEntity.DEFAULT_CAPACITY);
    }

    @Override public int getItemBarColor(ItemStack stack) { return StoredPEItem.itemBarColor(); }

    @Override public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        tooltip.add(StoredPEItem.energyTooltip(getStoredPE(stack), PEBlockEntity.DEFAULT_CAPACITY));
    }
}
