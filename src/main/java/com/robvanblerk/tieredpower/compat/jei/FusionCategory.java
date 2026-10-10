package com.robvanblerk.tieredpower.compat.jei;

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

import com.robvanblerk.tieredpower.block.entity.FusionReactorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Shows what the Fusion Reactor burns and what it produces. */
public class FusionCategory implements IRecipeCategory<FusionCategory.Display> {
	public record Display() {}

	private final IGuiHelper helper;
	private final IDrawable icon;
	/** Animated arrows are cached so they keep animating between frames. */
	private final Map<Integer, IDrawableAnimated> arrows = new HashMap<>();

	public FusionCategory(IGuiHelper helper) {
		this.helper = helper;
		
		this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.FUSION_REACTOR.get()));
	}

	@Override
	public RecipeType<Display> getRecipeType() {
		return TieredPowerJeiPlugin.FUSION;
	}

	@Override
	public Component getTitle() {
		return Component.translatable("block.tieredpower.fusion_reactor");
	}

	@Override
	public int getWidth() {
		return 170;
	}

	@Override
	public int getHeight() {
		return 56;
	}

	@Override
	public IDrawable getIcon() {
		return icon;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Display recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 6, 9).addItemStack(new ItemStack(ModBlocks.DEUTERIUM_CELL.get()));
		builder.addSlot(RecipeIngredientRole.INPUT, 26, 9).addItemStack(new ItemStack(ModBlocks.TRITIUM_CELL.get()));
		builder.addSlot(RecipeIngredientRole.OUTPUT, 90, 9).addItemStack(new ItemStack(ModBlocks.EMPTY_FUEL_CELL.get(), 2));
	}

	@Override
	public void draw(Display recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
		JeiDraw.slot(g, 6, 9);
		JeiDraw.slot(g, 26, 9);
		JeiDraw.bigSlot(g, 90, 9);
		JeiDraw.emptyArrow(g, 54, 8);
		arrows.computeIfAbsent(100, t -> JeiDraw.animatedArrow(helper, t)).draw(g, 54, 8);
		long total = (long) FusionReactorBlockEntity.MAX_OUTPUT * FusionReactorBlockEntity.BURN_TICKS;
		JeiDraw.text(g, "Up to " + JeiDraw.fe(FusionReactorBlockEntity.MAX_OUTPUT) + "/t", 116, 8);
		JeiDraw.text(g, FusionReactorBlockEntity.BURN_TICKS / 1200 + " min/pair", 116, 19);
		JeiDraw.text(g, String.format("~%,d FE per pair", total), 6, 33);
		JeiDraw.text(g, String.format("Ignition: %,d FE", FusionReactorBlockEntity.IGNITION_ENERGY), 6, 44);
	}
}
