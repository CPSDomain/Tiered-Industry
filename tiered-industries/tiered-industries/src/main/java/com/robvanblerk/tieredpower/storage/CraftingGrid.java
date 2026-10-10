package com.robvanblerk.tieredpower.storage;

import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Looks up what a 3x3 crafting grid would make, without a player or a real crafting table. */
public final class CraftingGrid {
	private static final CraftingContainer GRID = new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
		@Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
		@Override public boolean stillValid(Player p) { return false; }
	}, 3, 3);

	/** The result of crafting these 9 items (one each), or empty if they make nothing. */
	public static ItemStack result(Level level, List<ItemStack> nine) {
		for (int i = 0; i < 9; i++) GRID.setItem(i, i < nine.size() && !nine.get(i).isEmpty() ? nine.get(i).copyWithCount(1) : ItemStack.EMPTY);
		ItemStack out = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, GRID, level)
				.map(r -> r.assemble(GRID, level.registryAccess())).orElse(ItemStack.EMPTY);
		GRID.clearContent();
		return out;
	}

	/** The crafting recipe these 9 items make, if any. */
	public static java.util.Optional<net.minecraft.world.item.crafting.CraftingRecipe> recipe(Level level, List<ItemStack> nine) {
		for (int i = 0; i < 9; i++) GRID.setItem(i, i < nine.size() && !nine.get(i).isEmpty() ? nine.get(i).copyWithCount(1) : ItemStack.EMPTY);
		var r = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, GRID, level);
		GRID.clearContent();
		return r;
	}

	/**
	 * For each grid slot, the other items the recipe accepts there (e.g. every kind of planks), found from the recipe's
	 * ingredients. Empty lists where there's nothing else.
	 */
	public static List<List<ItemStack>> substitutes(Level level, List<ItemStack> nine) {
		List<List<ItemStack>> out = new java.util.ArrayList<>();
		var recipe = recipe(level, nine);
		for (int i = 0; i < 9; i++) {
			List<ItemStack> options = new java.util.ArrayList<>();
			ItemStack here = i < nine.size() ? nine.get(i) : ItemStack.EMPTY;
			if (!here.isEmpty() && recipe.isPresent()) {
				for (net.minecraft.world.item.crafting.Ingredient ing : recipe.get().getIngredients()) {
					if (ing.isEmpty() || !ing.test(here)) continue;
					for (ItemStack alt : ing.getItems()) {
						if (options.size() >= 48) break;
						if (!ItemStack.isSameItemSameTags(alt, here)) options.add(alt.copyWithCount(1));
					}
					break;
				}
			}
			out.add(options);
		}
		return out;
	}

	private CraftingGrid() {}
}
