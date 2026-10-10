package com.robvanblerk.tieredpower.energy;

import net.minecraftforge.energy.IEnergyStorage;

/** Exposes one energy buffer to a particular side as input-only or output-only. */
public class SidedEnergyView implements IEnergyStorage {
	private final IEnergyStorage base;
	private final boolean allowReceive;
	private final boolean allowExtract;

	public SidedEnergyView(IEnergyStorage base, boolean allowReceive, boolean allowExtract) {
		this.base = base;
		this.allowReceive = allowReceive;
		this.allowExtract = allowExtract;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		return allowReceive ? base.receiveEnergy(maxReceive, simulate) : 0;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		return allowExtract ? base.extractEnergy(maxExtract, simulate) : 0;
	}

	@Override
	public int getEnergyStored() {
		return base.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored() {
		return base.getMaxEnergyStored();
	}

	@Override
	public boolean canReceive() {
		return allowReceive && base.canReceive();
	}

	@Override
	public boolean canExtract() {
		return allowExtract && base.canExtract();
	}
}
