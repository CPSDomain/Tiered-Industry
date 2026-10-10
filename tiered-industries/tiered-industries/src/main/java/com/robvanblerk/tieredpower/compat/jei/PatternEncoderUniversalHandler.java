package com.robvanblerk.tieredpower.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.menu.PatternEncoderMenu;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.PatternFillPacket;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * JEI's + on any other recipe (Pulverizer, Alloy Smelter, furnace, other mods' machines...) while a Pattern Encoder is
 * open: copies its item inputs and outputs, with amounts, into a processing pattern. (Crafting recipes use
 * PatternEncoderTransferHandler instead.)
 */
public class PatternEncoderUniversalHandler implements IUniversalRecipeTransferHandler<PatternEncoderMenu> {
	@Override
	public Class<? extends PatternEncoderMenu> getContainerClass() {
		return PatternEncoderMenu.class;
	}

	@Override
	public Optional<MenuType<PatternEncoderMenu>> getMenuType() {
		return Optional.of(ModMenus.PATTERN_ENCODER.get());
	}

	private static ItemStack shown(IRecipeSlotView view) {
		return view.getDisplayedItemStack().or(() -> view.getItemStacks().findFirst()).orElse(ItemStack.EMPTY);
	}

	@Override
	public @Nullable IRecipeTransferError transferRecipe(PatternEncoderMenu menu, Object recipe, IRecipeSlotsView recipeSlots, Player player,
			boolean maxTransfer, boolean doTransfer) {
		if (!doTransfer) return null;
		List<ItemStack> ins = new ArrayList<>(), outs = new ArrayList<>(), fins = new ArrayList<>(), fouts = new ArrayList<>();
		for (IRecipeSlotView v : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
			ItemStack s = shown(v);
			if (!s.isEmpty() && ins.size() < 9) ins.add(s.copy());
			var f = v.getDisplayedIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK);
			if (f.isPresent() && !f.get().isEmpty() && fins.size() < 3) fins.add(com.robvanblerk.tieredpower.item.FluidDropItem.of(f.get().copy()));
		}
		for (IRecipeSlotView v : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			ItemStack s = shown(v);
			if (!s.isEmpty() && outs.size() < 3) outs.add(s.copy());
			var f = v.getDisplayedIngredient(mezz.jei.api.forge.ForgeTypes.FLUID_STACK);
			if (f.isPresent() && !f.get().isEmpty() && fouts.size() < 3) fouts.add(com.robvanblerk.tieredpower.item.FluidDropItem.of(f.get().copy()));
		}
		if ((ins.isEmpty() && fins.isEmpty()) || (outs.isEmpty() && fouts.isEmpty())) return null; // nothing we can encode
		if (recipe instanceof mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe) { // the Brewing Machine brews three bottles with one ingredient
			for (int i = 0; i < ins.size(); i++) if (ins.get(i).getItem() instanceof net.minecraft.world.item.PotionItem) ins.set(i, ins.get(i).copyWithCount(3));
			for (int i = 0; i < outs.size(); i++) if (outs.get(i).getItem() instanceof net.minecraft.world.item.PotionItem) outs.set(i, outs.get(i).copyWithCount(3));
		}
		ModNetwork.CHANNEL.sendToServer(new PatternFillPacket(menu.containerId, true, ins, outs, fins, fouts, machineFor(recipe)));
		return null;
	}

	/** The machine that runs this kind of recipe, for the pattern's Machine slot (empty if we don't know - set it by hand). */
	private static ItemStack machineFor(Object recipe) {
		if (recipe instanceof GasMachineCategory.Display d) return d.machine().copy();
		if (recipe instanceof mezz.jei.api.recipe.vanilla.IJeiBrewingRecipe) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.BREWING_MACHINE.get());
		if (!(recipe instanceof net.minecraft.world.item.crafting.Recipe<?> r)) return ItemStack.EMPTY;
		var t = r.getType();
		if (t == net.minecraft.world.item.crafting.RecipeType.SMELTING) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.ELECTRIC_FURNACE.get());
		if (t == net.minecraft.world.item.crafting.RecipeType.SMITHING) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.SMITHING_PRESS.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.PULVERIZING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.PULVERIZER.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.ALLOYING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.ALLOY_SMELTER.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.PURIFYING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.ORE_PURIFIER.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.COMPRESSING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.COMPRESSOR.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.SAWING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.ELECTRIC_SAWMILL.get());
		if (t == com.robvanblerk.tieredpower.registry.ModRecipes.CRUSHING.get()) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.ROCK_CRUSHER.get());
		return ItemStack.EMPTY;
	}
}
