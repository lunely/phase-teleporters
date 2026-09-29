package example.phaseteleporters.jei;

import example.phaseteleporters.EnrichmentRecipe;
import example.phaseteleporters.PhaseTeleportersMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class EnrichmentJeiCategory implements IRecipeCategory<EnrichmentRecipe> {
    private static final Identifier TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/enrichment_chamber.png");
    private final IDrawable icon;

    public EnrichmentJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(PhaseTeleportersMod.ENRICHMENT_CHAMBER));
    }

    @Override public RecipeType<EnrichmentRecipe> getRecipeType() {
        return InfusionJeiPlugin.ENRICHMENT_TYPE;
    }
    @Override public Text getTitle() {
        return Text.translatable("block.phaseteleporters.enrichment_chamber");
    }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 83; }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, EnrichmentRecipe recipe,
            IFocusGroup focuses) {
        builder.addInputSlot(65, 33)
                .addItemStacks(List.of(recipe.ingredient().getMatchingStacks()));
        builder.addOutputSlot(122, 33).addItemStack(recipe.result().copy());
    }

    @Override public void draw(EnrichmentRecipe recipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView slots,
            DrawContext context, double mouseX, double mouseY) {
        MachineJeiGraphics.drawMachineBackground(context, TEXTURE);
        context.drawTexture(TEXTURE, 90, 32, 178, 0, 24, 17, 256, 256);
    }
}
