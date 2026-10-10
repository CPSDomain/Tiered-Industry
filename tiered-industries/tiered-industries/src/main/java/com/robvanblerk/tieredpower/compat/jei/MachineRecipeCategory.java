package com.robvanblerk.tieredpower.compat.jei;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;

import com.robvanblerk.tieredpower.recipe.MachineRecipe;

/** One JEI category class used for the Ore Purifier, Compressor and Electric Sawmill. */
public class MachineRecipeCategory implements IRecipeCategory<MachineRecipe> {
	private final RecipeType<MachineRecipe> type;
	private final Component title;
	private final IGuiHelper helper;
	private final IDrawable icon;
	private final int energyPerTick;
	private final int water;
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	public MachineRecipeCategory(IGuiHelper helper, RecipeType<MachineRecipe> type, ItemLike machine, String titleKey, int energyPerTick, int water) {
		this.helper = helper;
		this.type = type;
		this.title = Component.translatable(titleKey);
		
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(machine));
		this.energyPerTick = energyPerTick;
		this.water = water;
	}

	@Override
	public RecipeType<MachineRecipe> getRecipeType() {
		return type;
	}

	@Override
	public Component getTitle() {
		return title;
	}

	@Override
	public int getWidth() {
		return 170;
	}

	@Override
	public int getHeight() {
		return 34;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 6, 9)
				.addItemStacks(Arrays.stream(recipe.getIngredient().getItems()).map(s -> s.copyWithCount(recipe.getCount())).toList());
		if (water > 0) {
			builder.addSlot(RecipeIngredientRole.INPUT, 26, 9).addFluidStack(Fluids.WATER, water).setFluidRenderer(water, false, 16, 16);
		}
		builder.addSlot(RecipeIngredientRole.OUTPUT, 80, 9).addItemStack(recipe.getResult());
		if (!recipe.getSecondary().isEmpty()) builder.addSlot(RecipeIngredientRole.OUTPUT, 104, 9).addItemStack(recipe.getSecondary());
	}

	@Override
	public void draw(MachineRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiDraw.slot(g, 6, 9);
		if (water > 0) JeiDraw.slot(g, 26, 9);
		JeiDraw.bigSlot(g, 80, 9);
		if (!recipe.getSecondary().isEmpty()) JeiDraw.slot(g, 104, 9);
		JeiDraw.emptyArrow(g, 46, 8);
		arrows.computeIfAbsent(recipe.getTime(), t -> JeiDraw.animatedArrow(helper, t)).draw(g, 46, 8);
		JeiDraw.text(g, JeiDraw.fe((long) recipe.getTime() * energyPerTick), 126, 8);
		JeiDraw.text(g, recipe.getTime() / 20.0 + "s", 126, 19);
	}
}
