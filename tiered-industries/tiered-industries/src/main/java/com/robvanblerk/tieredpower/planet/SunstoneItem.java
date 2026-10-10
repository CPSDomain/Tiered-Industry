package com.robvanblerk.tieredpower.planet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** Sunstone, from the Karoo: burns as long as 40 coal in any generator or furnace. */
public class SunstoneItem extends Item {
	public static final int BURN_TIME = 64_000;

	public SunstoneItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> type) {
		return BURN_TIME;
	}
}
