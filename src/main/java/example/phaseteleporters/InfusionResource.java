package example.phaseteleporters;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum InfusionResource {
    NONE(0, null, 0, 0xFF343A3F),
    REDSTONE(1, Items.REDSTONE, 10, 0xFFC83C32),
    DIAMOND(2, Items.DIAMOND, 10, 0xFF4CCBD4),
    GLOWSTONE_DUST(4, Items.GLOWSTONE_DUST, 10, 0xFFE4B83D),
    PURIFIED_OBSIDIAN_DUST(5, PhaseTeleportersMod.PURIFIED_OBSIDIAN_DUST, 10, 0xFF9654C8),
    COAL(6, Items.COAL, 10, 0xFF343A3F);

    private final int id;
    private final Item item;
    private final int unitsPerItem;
    private final int barColor;

    InfusionResource(int id, Item item, int unitsPerItem, int barColor) {
        this.id = id;
        this.item = item;
        this.unitsPerItem = unitsPerItem;
        this.barColor = barColor;
    }

    public int id() { return id; }
    public Item item() { return item; }
    public int unitsPerItem() { return unitsPerItem; }
    public int barColor() { return barColor; }

    public static InfusionResource fromStack(ItemStack stack) {
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_REDSTONE_DUST)) return REDSTONE;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_DIAMOND)) return DIAMOND;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_COAL)) return COAL;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_PURIFIED_OBSIDIAN_DUST)) return PURIFIED_OBSIDIAN_DUST;
        if (stack.isOf(Items.REDSTONE_BLOCK)) return REDSTONE;
        if (stack.isOf(Items.DIAMOND_BLOCK)) return DIAMOND;
        if (stack.isOf(Items.COAL_BLOCK) || stack.isOf(Items.CHARCOAL)) return COAL;
        if (stack.isIn(MaterialTags.DIAMOND_DUST)) return DIAMOND;
        if (stack.isIn(MaterialTags.REDSTONE_DUST)) return REDSTONE;
        if (stack.isIn(MaterialTags.GLOWSTONE_DUST)) return GLOWSTONE_DUST;
        if (stack.isIn(MaterialTags.COAL_DUST) || stack.isIn(MaterialTags.CHARCOAL_DUST)
                || stack.isIn(MaterialTags.CARBON_FRAGMENTS)) return COAL;
        for (InfusionResource resource : values()) {
            if (resource.item != null && stack.isOf(resource.item)) return resource;
        }
        return NONE;
    }

    public static int unitsForStack(ItemStack stack) {
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_REDSTONE_DUST)) return 80;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_DIAMOND)) return 100;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_COAL)) return 80;
        if (stack.isOf(PhaseTeleportersMod.ENRICHED_PURIFIED_OBSIDIAN_DUST)) return 80;
        if (stack.isOf(Items.REDSTONE_BLOCK)) return REDSTONE.unitsPerItem * 9;
        if (stack.isOf(Items.DIAMOND_BLOCK)) return DIAMOND.unitsPerItem * 9;
        if (stack.isOf(Items.COAL_BLOCK)) return 90;
        if (stack.isIn(MaterialTags.CARBON_FRAGMENTS)) return 5;
        if (stack.isOf(Items.CHARCOAL) || stack.isIn(MaterialTags.CHARCOAL_DUST)) return 20;
        return fromStack(stack).unitsPerItem;
    }

    public static InfusionResource byId(int id) {
        for (InfusionResource resource : values()) {
            if (resource.id == id) return resource;
        }
        return NONE;
    }
}
