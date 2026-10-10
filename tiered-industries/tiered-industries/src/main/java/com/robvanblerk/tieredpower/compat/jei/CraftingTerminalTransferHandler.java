package com.robvanblerk.tieredpower.compat.jei;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

import com.robvanblerk.tieredpower.menu.CraftingTerminalMenu;
import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.network.CraftingFillPacket;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.registry.ModMenus;
import com.robvanblerk.tieredpower.storage.ItemKey;

/** JEI's "+" button on crafting recipes while a Crafting Terminal is open: fills the grid from storage. */
public class CraftingTerminalTransferHandler implements IRecipeTransferHandler<CraftingTerminalMenu, CraftingRecipe> {
	private final IRecipeTransferHandlerHelper helper;

	public CraftingTerminalTransferHandler(IRecipeTransferHandlerHelper helper) {
		this.helper = helper;
	}

	@Override
	public Class<? extends CraftingTerminalMenu> getContainerClass() {
		return CraftingTerminalMenu.class;
	}

	@Override
	public Optional<MenuType<CraftingTerminalMenu>> getMenuType() {
		return Optional.of(ModMenus.CRAFTING_TERMINAL.get());
	}

	@Override
	public RecipeType<CraftingRecipe> getRecipeType() {
		return RecipeTypes.CRAFTING;
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(CraftingTerminalMenu menu, CraftingRecipe recipe, IRecipeSlotsView recipeSlots, Player player,
			boolean maxTransfer, boolean doTransfer) {
		List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);

		// What's available: storage, the player's inventory, and what's already in the grid (it gets put back first).
		Map<ItemKey, Long> available = new HashMap<>();
		for (StorageTerminalMenu.Entry e : menu.getClientList()) available.merge(new ItemKey(e.item()), e.count(), Long::sum);
		for (ItemStack s : player.getInventory().items) if (!s.isEmpty()) available.merge(new ItemKey(s), (long) s.getCount(), Long::sum);
		for (int i = 0; i < 9; i++) {
			ItemStack s = menu.craftGrid().getItem(i);
			if (!s.isEmpty()) available.merge(new ItemKey(s), (long) s.getCount(), Long::sum);
		}

		List<List<ItemStack>> slots = new ArrayList<>();
		List<IRecipeSlotView> missing = new ArrayList<>();
		for (IRecipeSlotView view : inputs) {
			List<ItemStack> options = view.getItemStacks().filter(s -> !s.isEmpty()).toList();
			slots.add(options);
			if (options.isEmpty()) continue;
			boolean found = false;
			for (ItemStack o : options) {
				ItemKey k = new ItemKey(o);
				Long have = available.get(k);
				if (have != null && have > 0) {
					available.put(k, have - 1);
					found = true;
					break;
				}
			}
			if (!found) missing.add(view);
		}
		while (slots.size() < 9) slots.add(List.of());

		if (!missing.isEmpty()) return helper.createUserErrorForMissingSlots(Component.literal("Missing items (not in storage or your inventory)"), missing);
		if (doTransfer) ModNetwork.CHANNEL.sendToServer(new CraftingFillPacket(menu.containerId, slots.subList(0, 9), maxTransfer));
		return null;
	}
}
