package example.phaseteleporters.jei;

import example.phaseteleporters.CrusherBlockEntity;
import example.phaseteleporters.PhaseTeleportersMod;
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
    private static final Identifier TEXTURE = Identifier.of("phaseteleporters", "textures/gui/crusher.png");
    private final IDrawable icon;

    public CrusherJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(PhaseTeleportersMod.CRUSHER));
    }

    @Override public RecipeType<CrusherBlockEntity.CrusherRecipe> getRecipeType() {
        return InfusionJeiPlugin.CRUSHER_TYPE;
    }

    @Override public Text getTitle() { return Text.translatable("block.phaseteleporters.crusher"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 83; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrusherBlockEntity.CrusherRecipe recipe,
            IFocusGroup focuses) {
        builder.addInputSlot(89, 15).addItemStack(new ItemStack(recipe.input()));
        builder.addOutputSlot(89, 55).addItemStack(new ItemStack(recipe.output()));
    }

    @Override
    public void draw(CrusherBlockEntity.CrusherRecipe recipe, IRecipeSlotsView slots,
            DrawContext context, double mouseX, double mouseY) {
        MachineJeiGraphics.drawMachineBackground(context, TEXTURE);
        drawProgressArrow(context, 89, 34);
    }

    private static void drawProgressArrow(DrawContext context, int x, int y) {
        for (int row = 0; row < 18; row++) {
            if (row <= 9) context.fill(x + 6, y + row, x + 9, y + row + 1, 0xFFFFFFFF);
            else {
                int headRow = row - 10;
                context.fill(x + headRow, y + row, x + 15 - headRow, y + row + 1, 0xFFFFFFFF);
            }
        }
    }
}
