package com.robvanblerk.tieredpower.energy;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyUtil {
	/** Finds the energy storage of the block on the given side of pos, as seen from our side. */
	public static @Nullable IEnergyStorage neighbour(Level level, BlockPos pos, Direction dir) {
		BlockEntity be = level.getBlockEntity(pos.relative(dir));
		if (be == null) return null;
		return be.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
	}

	/** Moves up to maxAmount FE from one storage to another. Returns how much actually moved. */
	public static int move(IEnergyStorage from, @Nullable IEnergyStorage to, int maxAmount) {
		if (to == null || maxAmount <= 0 || !to.canReceive()) return 0;
		int available = from.extractEnergy(maxAmount, true);
		if (available <= 0) return 0;
		int accepted = to.receiveEnergy(available, false);
		if (accepted <= 0) return 0;
		return from.extractEnergy(accepted, false);
	}

	private EnergyUtil() {}
}
