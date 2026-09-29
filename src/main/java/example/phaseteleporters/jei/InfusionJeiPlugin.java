package example.phaseteleporters.jei;

import example.phaseteleporters.InfusionStationBlockEntity;
import example.phaseteleporters.CrusherBlockEntity;
import example.phaseteleporters.EnrichmentRecipe;
import example.phaseteleporters.PhaseTeleportersMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.SingleStackRecipeInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JeiPlugin
public final class InfusionJeiPlugin implements IModPlugin {
    private boolean enrichmentRegistered;
    private boolean furnaceRegistered;
    public static final RecipeType<InfusionStationBlockEntity.InfusionRecipe> TYPE =
            RecipeType.create(PhaseTeleportersMod.MOD_ID, "infusion", InfusionStationBlockEntity.InfusionRecipe.class);
    public static final RecipeType<CrusherBlockEntity.CrusherRecipe> CRUSHER_TYPE =
            RecipeType.create(PhaseTeleportersMod.MOD_ID, "crusher", CrusherBlockEntity.CrusherRecipe.class);
    public static final RecipeType<EnrichmentRecipe> ENRICHMENT_TYPE =
            RecipeType.create(PhaseTeleportersMod.MOD_ID, "enrichment", EnrichmentRecipe.class);
    public static final RecipeType<ElectricFurnaceJeiCategory.Recipe> ELECTRIC_FURNACE_TYPE =
            RecipeType.create(PhaseTeleportersMod.MOD_ID, "electric_furnace", ElectricFurnaceJeiCategory.Recipe.class);

    @Override public Identifier getPluginUid() {
        return Identifier.of(PhaseTeleportersMod.MOD_ID, "infusion_jei");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InfusionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrusherJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new EnrichmentJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ElectricFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, InfusionStationBlockEntity.RECIPES);
        registration.addRecipes(CRUSHER_TYPE, CrusherBlockEntity.RECIPES);
        var world = MinecraftClient.getInstance().world;
        if (world != null) {
            registration.addRecipes(ENRICHMENT_TYPE, world.getRecipeManager()
                    .listAllOfType(PhaseTeleportersMod.ENRICHMENT_RECIPE_TYPE).stream()
                    .map(RecipeEntry::value).toList());
            enrichmentRegistered = true;
            registration.addRecipes(ELECTRIC_FURNACE_TYPE, furnaceRecipes(world));
            furnaceRegistered = true;
        }
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        var world = MinecraftClient.getInstance().world;
        if (!enrichmentRegistered && world != null) {
            runtime.getRecipeManager().addRecipes(ENRICHMENT_TYPE, world.getRecipeManager()
                    .listAllOfType(PhaseTeleportersMod.ENRICHMENT_RECIPE_TYPE).stream()
                    .map(RecipeEntry::value).toList());
            enrichmentRegistered = true;
        }
        if (!furnaceRegistered && world != null) {
            runtime.getRecipeManager().addRecipes(ELECTRIC_FURNACE_TYPE, furnaceRecipes(world));
            furnaceRegistered = true;
        }
    }

    @Override public void onRuntimeUnavailable() {
        enrichmentRegistered = false;
        furnaceRegistered = false;
    }

    private static List<ElectricFurnaceJeiCategory.Recipe> furnaceRecipes(ClientWorld world) {
        List<ElectricFurnaceJeiCategory.Recipe> recipes = new ArrayList<>();
        var manager = world.getRecipeManager();
        for (var entry : manager.listAllOfType(net.minecraft.recipe.RecipeType.SMELTING)) {
            List<ItemStack> inputs = Arrays.stream(entry.value().getIngredients().getFirst().getMatchingStacks())
                    .map(ItemStack::copy).toList();
            if (!inputs.isEmpty()) {
                recipes.add(new ElectricFurnaceJeiCategory.Recipe(entry.id(), inputs,
                        entry.value().getResult(world.getRegistryManager()).copy()));
            }
        }
        for (var entry : manager.listAllOfType(net.minecraft.recipe.RecipeType.BLASTING)) {
            List<ItemStack> inputs = Arrays.stream(entry.value().getIngredients().getFirst().getMatchingStacks())
                    .filter(stack -> manager.getFirstMatch(net.minecraft.recipe.RecipeType.SMELTING,
                            new SingleStackRecipeInput(stack), world).isEmpty())
                    .map(ItemStack::copy).toList();
            if (!inputs.isEmpty()) {
                recipes.add(new ElectricFurnaceJeiCategory.Recipe(entry.id(), inputs,
                        entry.value().getResult(world.getRegistryManager()).copy()));
            }
        }
        return recipes;
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(PhaseTeleportersMod.INFUSION_STATION, TYPE);
        registration.addRecipeCatalyst(PhaseTeleportersMod.CRUSHER, CRUSHER_TYPE);
        registration.addRecipeCatalyst(PhaseTeleportersMod.ENRICHMENT_CHAMBER, ENRICHMENT_TYPE);
        registration.addRecipeCatalyst(PhaseTeleportersMod.ELECTRIC_FURNACE, ELECTRIC_FURNACE_TYPE);
    }
}
