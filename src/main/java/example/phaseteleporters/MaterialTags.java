package example.phaseteleporters;

import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class MaterialTags {
    public static final TagKey<Item> DIAMOND_DUST = common("dusts/diamond");
    public static final TagKey<Item> COAL_DUST = common("dusts/coal");
    public static final TagKey<Item> CHARCOAL_DUST = common("dusts/charcoal");
    public static final TagKey<Item> OBSIDIAN_DUST = common("dusts/obsidian");
    public static final TagKey<Item> REDSTONE_DUST = common("dusts/redstone");
    public static final TagKey<Item> GLOWSTONE_DUST = common("dusts/glowstone");
    public static final TagKey<Item> CARBON_FRAGMENTS = TagKey.of(
            RegistryKeys.ITEM, Identifier.of("phaseteleporters", "infusion/carbon_fragments"));

    private MaterialTags() {}

    private static TagKey<Item> common(String path) {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of("c", path));
    }
}
