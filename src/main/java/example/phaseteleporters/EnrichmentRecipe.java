package example.phaseteleporters;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public record EnrichmentRecipe(Ingredient ingredient, ItemStack result)
        implements Recipe<SingleStackRecipeInput> {

    @Override public boolean matches(SingleStackRecipeInput input, World world) {
        return ingredient.test(input.item());
    }

    @Override public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return result.copy();
    }

    @Override public boolean fits(int width, int height) { return width * height >= 1; }
    @Override public ItemStack getResult(RegistryWrapper.WrapperLookup lookup) { return result.copy(); }
    @Override public DefaultedList<Ingredient> getIngredients() {
        DefaultedList<Ingredient> ingredients = DefaultedList.of();
        ingredients.add(ingredient);
        return ingredients;
    }
    @Override public RecipeSerializer<?> getSerializer() { return PhaseTeleportersMod.ENRICHMENT_RECIPE_SERIALIZER; }
    @Override public RecipeType<?> getType() { return PhaseTeleportersMod.ENRICHMENT_RECIPE_TYPE; }

    public static final class Serializer implements RecipeSerializer<EnrichmentRecipe> {
        private static final MapCodec<EnrichmentRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter(EnrichmentRecipe::ingredient),
                        ItemStack.VALIDATED_CODEC.fieldOf("result").forGetter(EnrichmentRecipe::result)
                ).apply(instance, EnrichmentRecipe::new));
        private static final PacketCodec<RegistryByteBuf, EnrichmentRecipe> PACKET_CODEC = PacketCodec.tuple(
                Ingredient.PACKET_CODEC, EnrichmentRecipe::ingredient,
                ItemStack.PACKET_CODEC, EnrichmentRecipe::result,
                EnrichmentRecipe::new);

        @Override public MapCodec<EnrichmentRecipe> codec() { return CODEC; }
        @Override public PacketCodec<RegistryByteBuf, EnrichmentRecipe> packetCodec() { return PACKET_CODEC; }
    }
}
