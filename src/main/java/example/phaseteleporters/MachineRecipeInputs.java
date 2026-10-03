package example.phaseteleporters;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.world.World;

/** Uses the current recipe manager, including recipes supplied by other mods and data packs. */
public final class MachineRecipeInputs {
    private MachineRecipeInputs() {}

    public static boolean furnace(World world, ItemStack stack) {
        if (world == null || stack.isEmpty()) return false;
        var input = new SingleStackRecipeInput(stack);
        return world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, input, world).isPresent()
                || world.getRecipeManager().getFirstMatch(RecipeType.BLASTING, input, world).isPresent();
    }

    public static boolean enrichment(World world, ItemStack stack) {
        return world != null && !stack.isEmpty() && world.getRecipeManager().getFirstMatch(
                PhaseTeleportersMod.ENRICHMENT_RECIPE_TYPE,
                new SingleStackRecipeInput(stack), world).isPresent();
    }
}
