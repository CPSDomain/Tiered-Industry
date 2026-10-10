package com.robvanblerk.tieredpower.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import com.robvanblerk.tieredpower.menu.PatternEncoderMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.PatternFillPacket;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** JEI's "+" on a crafting recipe while a Pattern Encoder is open: copies the recipe into its grid (uses nothing up). */
public class PatternEncoderTransferHandler implements IRecipeTransferHandler<PatternEncoderMenu, CraftingRecipe> {
	@Override
	public Class<? extends PatternEncoderMenu> getContainerClass() {
		return PatternEncoderMenu.class;
	}

	@Override
	public Optional<MenuType<PatternEncoderMenu>> getMenuType() {
		return Optional.of(ModMenus.PATTERN_ENCODER.get());
	}

	@Override
	public RecipeType<CraftingRecipe> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(PatternEncoderMenu menu, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player,
			boolean maxTransfer, boolean doTransfer) {
		if (!doTransfer) return null;
		List<ItemStack> items = new ArrayList<>(9);
		for (IRecipeSlotView view : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
			items.add(view.getItemStacks().filter(s -> !s.isEmpty()).findFirst().orElse(ItemStack.EMPTY));
		}
		while (items.size() < 9) items.add(ItemStack.EMPTY);
		ModNetwork.CHANNEL.sendToServer(new PatternFillPacket(menu.containerId, items.subList(0, 9)));
		return null;
	}
}
