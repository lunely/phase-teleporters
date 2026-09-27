package example.phaseteleports.jei;

import example.phaseteleports.CrusherBlockEntity;
import example.phaseteleports.EnergyBarRenderer;
import example.phaseteleports.PhaseTeleportsMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CrusherJeiCategory implements IRecipeCategory<CrusherBlockEntity.CrusherRecipe> {
    private static final Identifier TEXTURE = Identifier.of("phaseteleports", "textures/gui/crusher.png");
    private static final int GUI_X = 24;
    private final IDrawable icon;

    public CrusherJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(PhaseTeleportsMod.CRUSHER));
    }

    @Override public RecipeType<CrusherBlockEntity.CrusherRecipe> getRecipeType() {
        return InfusionJeiPlugin.CRUSHER_TYPE;
    }

    @Override public Text getTitle() { return Text.translatable("block.phaseteleports.crusher"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 200; }
    @Override public int getHeight() { return 83; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrusherBlockEntity.CrusherRecipe recipe,
            IFocusGroup focuses) {
        builder.addInputSlot(GUI_X + 79, 14).addItemStack(new ItemStack(recipe.input()));
        builder.addOutputSlot(GUI_X + 79, 55).addItemStack(new ItemStack(recipe.output()));
    }

    @Override
    public void draw(CrusherBlockEntity.CrusherRecipe recipe, IRecipeSlotsView slots,
            DrawContext context, double mouseX, double mouseY) {
        context.drawTexture(TEXTURE, GUI_X, 0, 0, 0, 176, 83);
        EnergyBarRenderer.draw(context, GUI_X - 20, 20, 0, 100_000);
        drawProgressArrow(context, GUI_X + 80, 35);
    }

    private static void drawProgressArrow(DrawContext context, int x, int y) {
        for (int row = 0; row < 17; row++) {
            if (row <= 8) context.fill(x + 6, y + row, x + 9, y + row + 1, 0xFFFFFFFF);
            else {
                int headRow = row - 9;
                context.fill(x + headRow, y + row, x + 15 - headRow, y + row + 1, 0xFFFFFFFF);
            }
        }
    }
}
