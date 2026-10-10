package com.robvanblerk.tieredpower.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** An item that burns in furnaces, the Coal Generator and the Boiler (e.g. Sawdust). */
public class FuelItem extends Item {
	private final int burnTime;

	public FuelItem(int burnTime, Properties properties) {
		super(properties);
		this.burnTime = burnTime;
	}

	@Override
	public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
		return burnTime;
	}
}
