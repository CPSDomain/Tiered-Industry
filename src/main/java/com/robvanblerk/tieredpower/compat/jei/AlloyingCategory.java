package com.robvanblerk.tieredpower.compat.jei;

import java.util.Arrays;
import java.util.List;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import java.util.HashMap;
import java.util.Map;

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
import net.minecraft.world.item.crafting.Ingredient;

import com.robvanblerk.tieredpower.block.entity.AlloySmelterBlockEntity;
import com.robvanblerk.tieredpower.recipe.AlloyingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlocks;

public class AlloyingCategory implements IRecipeCategory<AlloyingRecipe> {
	private final IGuiHelper helper;
	private final IDrawable icon;
	/** Animated arrows are cached so they keep animating between frames. */
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	public AlloyingCategory(IGuiHelper helper) {
		this.helper = helper;
		
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ALLOY_SMELTER.get()));
	}

	@Override
	public RecipeType<AlloyingRecipe> getRecipeType() {
		return TieredPowerJeiPlugin.ALLOYING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.tieredpower.alloy_smelter");
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

	/** Shows each accepted item with the stack size the recipe needs. */
	private static List<ItemStack> withCount(Ingredient ingredient, int count) {
		return Arrays.stream(ingredient.getItems()).map(s -> s.copyWithCount(count)).toList();
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, AlloyingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 6, 9).addItemStacks(withCount(recipe.getFirst(), recipe.getFirstCount()));
		builder.addSlot(RecipeIngredientRole.INPUT, 26, 9).addItemStacks(withCount(recipe.getSecond(), recipe.getSecondCount()));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 9).addItemStack(recipe.getResult());
	}

	@Override
	public void draw(AlloyingRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiDraw.slot(g, 6, 9);
		JeiDraw.slot(g, 26, 9);
		JeiDraw.bigSlot(g, 90, 9);
		JeiDraw.emptyArrow(g, 54, 8);
		arrows.computeIfAbsent(recipe.getTime(), t -> JeiDraw.animatedArrow(helper, t)).draw(g, 54, 8);
		JeiDraw.text(g, JeiDraw.fe((long) recipe.getTime() * AlloySmelterBlockEntity.ENERGY_PER_TICK), 116, 8);
		JeiDraw.text(g, recipe.getTime() / 20.0 + "s", 116, 19);
	}
}
