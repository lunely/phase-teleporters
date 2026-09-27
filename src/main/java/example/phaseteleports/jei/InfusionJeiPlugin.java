package example.phaseteleports.jei;

import example.phaseteleports.InfusionStationBlockEntity;
import example.phaseteleports.CrusherBlockEntity;
import example.phaseteleports.PhaseTeleportsMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.util.Identifier;

@JeiPlugin
public final class InfusionJeiPlugin implements IModPlugin {
    public static final RecipeType<InfusionStationBlockEntity.InfusionRecipe> TYPE =
            RecipeType.create(PhaseTeleportsMod.MOD_ID, "infusion", InfusionStationBlockEntity.InfusionRecipe.class);
    public static final RecipeType<CrusherBlockEntity.CrusherRecipe> CRUSHER_TYPE =
            RecipeType.create(PhaseTeleportsMod.MOD_ID, "crusher", CrusherBlockEntity.CrusherRecipe.class);

    @Override public Identifier getPluginUid() {
        return Identifier.of(PhaseTeleportsMod.MOD_ID, "infusion_jei");
    }

    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new InfusionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrusherJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TYPE, InfusionStationBlockEntity.RECIPES);
        registration.addRecipes(CRUSHER_TYPE, CrusherBlockEntity.RECIPES);
    }

    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(PhaseTeleportsMod.INFUSION_STATION, TYPE);
        registration.addRecipeCatalyst(PhaseTeleportsMod.CRUSHER, CRUSHER_TYPE);
        registration.addRecipeCatalyst(PhaseTeleportsMod.ELECTRIC_FURNACE, RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(PhaseTeleportsMod.ELECTRIC_FURNACE, RecipeTypes.BLASTING);
    }
}
