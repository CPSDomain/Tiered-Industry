package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** Public door to the package-private GasTanks push helper (it works for any fluid). */
public final class PushHelper {
	public static int pushFluid(Level level, BlockPos pos, Fluid fluid, int amount) {
		return GasTanks.push(level, pos, fluid, amount);
	}

	private PushHelper() {}
}
