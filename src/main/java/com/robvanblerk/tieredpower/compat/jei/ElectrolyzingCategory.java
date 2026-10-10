package com.robvanblerk.tieredpower.compat.jei;

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
import net.minecraft.world.level.material.Fluids;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** The Electrolyzer's two fixed recipes (Deuterium from water, Tritium from lithium). */
public class ElectrolyzingCategory implements IRecipeCategory<ElectrolyzingCategory.Display> {
	/** One Electrolyzer recipe as shown in JEI. waterMb = 0 means the second input is lithium instead. */
	public record Display(List<ItemStack> secondInput, int waterMb, ItemStack output, int ticks, int energy) {}

	private final IGuiHelper helper;
	private final IDrawable icon;
	/** Animated arrows are cached so they keep animating between frames. */
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	public ElectrolyzingCategory(IGuiHelper helper) {
		this.helper = helper;
		
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.ELECTROLYZER.get()));
	}

	@Override
	public RecipeType<Display> getRecipeType() {
		return TieredPowerJeiPlugin.ELECTROLYZING;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.tieredpower.electrolyzer");
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
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 6, 9).addItemStack(new ItemStack(ModBlocks.EMPTY_FUEL_CELL.get()));
		if (recipe.waterMb() > 0) {
			builder.addSlot(RecipeIngredientRole.INPUT, 26, 9)
					.addFluidStack(Fluids.WATER, recipe.waterMb())
					.setFluidRenderer(recipe.waterMb(), false, 16, 16);
		} else {
			builder.addSlot(RecipeIngredientRole.INPUT, 26, 9).addItemStacks(recipe.secondInput());
		}
		builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 9).addItemStack(recipe.output());
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiDraw.slot(g, 6, 9);
		JeiDraw.slot(g, 26, 9);
		JeiDraw.bigSlot(g, 90, 9);
		JeiDraw.emptyArrow(g, 54, 8);
		arrows.computeIfAbsent(recipe.ticks(), t -> JeiDraw.animatedArrow(helper, t)).draw(g, 54, 8);
		JeiDraw.text(g, JeiDraw.fe(recipe.energy()), 116, 8);
		JeiDraw.text(g, recipe.ticks() / 20.0 + "s", 116, 19);
	}
}
