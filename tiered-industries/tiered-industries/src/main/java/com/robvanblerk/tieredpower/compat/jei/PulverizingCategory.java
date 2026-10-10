package com.robvanblerk.tieredpower.compat.jei;

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
import mezz.jei.api.constants.VanillaTypes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.PulverizerBlockEntity;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlocks;

public class PulverizingCategory implements IRecipeCategory<PulverizingRecipe> {
	private final IGuiHelper helper;
	private final IDrawable icon;
	/** Animated arrows are cached so they keep animating between frames. */
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	public PulverizingCategory(IGuiHelper helper) {
		this.helper = helper;
		
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.PULVERIZER.get()));
	}

	@Override
	public RecipeType<PulverizingRecipe> getRecipeType() {
		return TieredPowerJeiPlugin.PULVERIZING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.tieredpower.pulverizer");
	}

	@Override
	public int getWidth() {
		return 150;
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
	public void setRecipe(IRecipeLayoutBuilder builder, PulverizingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 6, 9).addIngredients(recipe.getIngredient());
		builder.addSlot(RecipeIngredientRole.OUTPUT, 70, 9).addItemStack(recipe.getResult());
	}

	@Override
	public void draw(PulverizingRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiDraw.slot(g, 6, 9);
		JeiDraw.bigSlot(g, 70, 9);
		JeiDraw.emptyArrow(g, 34, 8);
		arrows.computeIfAbsent(recipe.getTime(), t -> JeiDraw.animatedArrow(helper, t)).draw(g, 34, 8);
		JeiDraw.text(g, JeiDraw.fe((long) recipe.getTime() * PulverizerBlockEntity.ENERGY_PER_TICK), 96, 8);
		JeiDraw.text(g, recipe.getTime() / 20.0 + "s", 96, 19);
	}
}
