package example.phaseteleporters.jei;

import example.phaseteleporters.ElectricFurnaceBlockEntity;
import example.phaseteleporters.PhaseTeleportersMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ElectricFurnaceJeiCategory implements IRecipeCategory<ElectricFurnaceJeiCategory.Recipe> {
    private static final Identifier TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace.png");
    private final IDrawable icon;

    public record Recipe(Identifier id, List<ItemStack> inputs, ItemStack output) {}

    public ElectricFurnaceJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(PhaseTeleportersMod.ELECTRIC_FURNACE));
    }

    @Override public RecipeType<Recipe> getRecipeType() { return InfusionJeiPlugin.ELECTRIC_FURNACE_TYPE; }
    @Override public Text getTitle() {
        return Text.translatable("block.phaseteleporters.electric_furnace");
    }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 83; }
    @Override public Identifier getRegistryName(Recipe recipe) { return recipe.id(); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Recipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(65, 33).addItemStacks(recipe.inputs());
        builder.addOutputSlot(122, 33).addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(Recipe recipe, IRecipeSlotsView slots, DrawContext context, double mouseX, double mouseY) {
        MachineJeiGraphics.drawMachineBackground(context, TEXTURE);
        context.drawTexture(TEXTURE, 90, 32, 178, 0, 24, 17, 256, 256);
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        Text duration = Text.translatable("gui.phaseteleporters.jei.smelting_time",
                ElectricFurnaceBlockEntity.PROCESS_TIME / 20);
        context.drawText(textRenderer, duration, (getWidth() - textRenderer.getWidth(duration)) / 2,
                66, 0xFF404040, false);
    }
}
