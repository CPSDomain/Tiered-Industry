package com.robvanblerk.tieredpower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Small helpers for machines that keep gases as plain mB counters. */
final class GasTanks {
	/** Pushes up to 'amount' of a fluid into neighbours; returns how much left. */
	static int push(Level level, BlockPos pos, Fluid fluid, int amount) {
		for (Direction dir : Direction.values()) {
			if (amount <= 0) break;
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next)) continue;
			BlockEntity n = level.getBlockEntity(next);
			if (n == null) continue;
			IFluidHandler target = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
			if (target == null) continue;
			amount -= target.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
		}
		return amount;
	}

	/** Pulls up to 'amount' of a fluid from neighbours; returns how much was pulled. */
	static int pull(Level level, BlockPos pos, Fluid fluid, int amount) {
		int got = 0;
		for (Direction dir : Direction.values()) {
			if (got >= amount) break;
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next)) continue;
			BlockEntity n = level.getBlockEntity(next);
			if (n == null) continue;
			IFluidHandler source = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
			if (source == null) continue;
			got += source.drain(new FluidStack(fluid, amount - got), IFluidHandler.FluidAction.EXECUTE).getAmount();
		}
		return got;
	}

	private GasTanks() {}
}
