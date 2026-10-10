package com.robvanblerk.tieredpower.industry;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Coal Coke: burns twice as long as coal, and turns iron into steel in the Industrial Blast Furnace. */
public class CoalCokeItem extends Item {
	public CoalCokeItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> type) {
		return 3200;
	}
}
