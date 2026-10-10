package com.robvanblerk.tieredpower.energy;

import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Fission fuel: uranium rods, and MOX rods (uranium + plutonium) that last twice as long and run 50% harder and hotter. */
public final class FuelRods {
	public static boolean isFuel(ItemStack stack) {
		return stack.is(ModBlocks.URANIUM_FUEL_ROD.get()) || stack.is(ModBlocks.MOX_FUEL_ROD.get());
	}

	public static boolean isMox(ItemStack stack) {
		return stack.is(ModBlocks.MOX_FUEL_ROD.get());
	}

	private FuelRods() {}
}
