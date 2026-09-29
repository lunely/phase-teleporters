package example.phaseteleporters.jei;

import com.mojang.blaze3d.systems.RenderSystem;
import example.phaseteleporters.InfusionResource;
import example.phaseteleporters.InfusionStationBlockEntity;
import example.phaseteleporters.PhaseTeleportersMod;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class InfusionJeiCategory implements IRecipeCategory<InfusionStationBlockEntity.InfusionRecipe> {
    private static final Identifier GUI_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/infusion_station.png");
    private static final Identifier ENERGY_SLOT_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/slot/energy_slot.png");
    private static final Identifier EMPTY_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_empty.png");
    private static final Identifier FILLED_ENERGY_TEXTURE =
            Identifier.of("phaseteleporters", "textures/gui/electric_furnace_energy_filled.png");
    private static final Identifier WATER_TEXTURE =
            Identifier.of("minecraft", "textures/block/water_still.png");
    private static final int BAR_Y = 8;
    private static final int BAR_HEIGHT = 48;
    private final IDrawable icon;

    public InfusionJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemStack(new ItemStack(PhaseTeleportersMod.INFUSION_STATION));
    }

    @Override public RecipeType<InfusionStationBlockEntity.InfusionRecipe> getRecipeType() {
        return InfusionJeiPlugin.TYPE;
    }

    @Override public Text getTitle() {
        return Text.translatable("block.phaseteleporters.infusion_station");
    }

    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 83; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InfusionStationBlockEntity.InfusionRecipe recipe,
            IFocusGroup focuses) {
        builder.addInputSlot(33, 62).addItemStacks(infusionInputs(recipe));
        builder.addInputSlot(77, 31).addItemStack(new ItemStack(recipe.input()));
        builder.addOutputSlot(129, 31).addItemStack(new ItemStack(recipe.output()));
    }

    private static List<ItemStack> infusionInputs(InfusionStationBlockEntity.InfusionRecipe recipe) {
        InfusionResource resource = recipe.resource();
        List<ItemStack> stacks = new ArrayList<>();
        addInfusionInput(stacks, resource.item(), recipe.units());
        for (Item item : Registries.ITEM) {
            if (item != resource.item() && InfusionResource.fromStack(new ItemStack(item)) == resource) {
                addInfusionInput(stacks, item, recipe.units());
            }
        }
        return stacks;
    }

    private static void addInfusionInput(List<ItemStack> stacks, Item item, int requiredUnits) {
        int units = InfusionResource.unitsForStack(new ItemStack(item));
        if (units > 0) stacks.add(new ItemStack(item, Math.ceilDiv(requiredUnits, units)));
    }

    @Override
    public void draw(InfusionStationBlockEntity.InfusionRecipe recipe, IRecipeSlotsView slots,
            DrawContext context, double mouseX, double mouseY) {
        context.drawTexture(GUI_TEXTURE, 0, 0, 0, 0, 176, 83);
        context.fill(7, 7, 25, 57, 0xFF373737);
        context.drawTexture(EMPTY_ENERGY_TEXTURE, 8, BAR_Y, 0, 0, 16, BAR_HEIGHT, 16, BAR_HEIGHT);
        context.drawTexture(FILLED_ENERGY_TEXTURE, 8, BAR_Y, 0, 0, 16, BAR_HEIGHT, 16, BAR_HEIGHT);
        drawInfusionPreview(context, recipe.resource());
        context.drawTexture(ENERGY_SLOT_TEXTURE, 8, 62, 0, 0, 16, 16, 16, 16);
        context.drawTexture(GUI_TEXTURE, 100, 31, 180, 4, 22, 15, 256, 256);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, InfusionStationBlockEntity.InfusionRecipe recipe,
            IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 33 && mouseX < 49 && mouseY >= BAR_Y && mouseY < BAR_Y + BAR_HEIGHT) {
            tooltip.add(recipe.resource().item().getName().copy().append(": " + recipe.units()));
        }
    }

    private static void drawInfusionPreview(DrawContext context, InfusionResource resource) {
        int color = resource.barColor();
        int red = (((color >> 16) & 0xFF) * 3 + 0x8B * 2) / 5;
        int green = (((color >> 8) & 0xFF) * 3 + 0x8B * 2) / 5;
        int blue = ((color & 0xFF) * 3 + 0x8B * 2) / 5;
        context.fill(33, BAR_Y, 49, BAR_Y + BAR_HEIGHT,
                0xFF000000 | red << 16 | green << 8 | blue);
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 0.38F);
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            int frame = client.world != null ? (int) ((client.world.getTime() / 5) % 32) : 0;
            for (int segmentTop = BAR_Y; segmentTop < BAR_Y + BAR_HEIGHT; segmentTop += 16) {
                context.drawTexture(WATER_TEXTURE, 33, segmentTop, 0, frame * 16,
                        16, 16, 16, 512);
            }
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        for (int tick = 0; tick < 16; tick++) {
            int tickWidth = (tick & 1) == 0 ? 6 : 8;
            int tickY = BAR_Y + 1 + tick * 3;
            context.fill(33, tickY, 33 + tickWidth, tickY + 1, 0xFFB31212);
        }
    }
}
