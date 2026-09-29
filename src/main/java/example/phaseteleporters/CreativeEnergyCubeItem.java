package example.phaseteleporters;

import example.phaseteleporters.energy.StoredPEItem;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

/** Always shows a full energy bar on the creative cube item. */
public final class CreativeEnergyCubeItem extends BlockItem {
    public CreativeEnergyCubeItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override public boolean isItemBarVisible(ItemStack stack) { return true; }
    @Override public int getItemBarStep(ItemStack stack) { return 13; }
    @Override public int getItemBarColor(ItemStack stack) { return StoredPEItem.itemBarColor(); }
}
