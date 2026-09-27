package example.phaseteleports;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public enum InfusionResource {
    NONE(0, null, 0, 0xFF343A3F),
    REDSTONE(1, Items.REDSTONE, 10, 0xFFC83C32),
    DIAMOND(2, Items.DIAMOND, 100, 0xFF4CCBD4),
    ENDER_PEARL(3, Items.ENDER_PEARL, 100, 0xFF9654C8),
    GLOWSTONE_DUST(4, Items.GLOWSTONE_DUST, 10, 0xFFE4B83D),
    PURIFIED_OBSIDIAN_DUST(5, PhaseTeleportsMod.PURIFIED_OBSIDIAN_DUST, 100, 0xFF9654C8);

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
        if (stack.isOf(Items.REDSTONE_BLOCK)) return REDSTONE;
        if (stack.isOf(Items.DIAMOND_BLOCK)) return DIAMOND;
        for (InfusionResource resource : values()) {
            if (resource.item != null && stack.isOf(resource.item)) return resource;
        }
        return NONE;
    }

    public static int unitsForStack(ItemStack stack) {
        if (stack.isOf(Items.REDSTONE_BLOCK)) return REDSTONE.unitsPerItem * 9;
        if (stack.isOf(Items.DIAMOND_BLOCK)) return DIAMOND.unitsPerItem * 9;
        return fromStack(stack).unitsPerItem;
    }

    public static InfusionResource byId(int id) {
        for (InfusionResource resource : values()) {
            if (resource.id == id) return resource;
        }
        return NONE;
    }
}
