package com.robvanblerk.tieredpower.energy;

import net.minecraftforge.energy.EnergyStorage;

/**
 * Forge's standard energy buffer, plus a callback so the owning block entity knows to save,
 * and helpers for the block entity to add/remove energy directly (ignoring the per-call limits,
 * which only apply to other blocks talking to us).
 */
public class ModEnergyStorage extends EnergyStorage {
	private final Runnable onChanged;

	public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
		super(capacity, maxReceive, maxExtract);
		this.onChanged = onChanged;
	}

	@Override
	public int receiveEnergy(int maxReceive, boolean simulate) {
		int received = super.receiveEnergy(maxReceive, simulate);
		if (received > 0 && !simulate) onChanged.run();
		return received;
	}

	@Override
	public int extractEnergy(int maxExtract, boolean simulate) {
		int extracted = super.extractEnergy(maxExtract, simulate);
		if (extracted > 0 && !simulate) onChanged.run();
		return extracted;
	}

	public void setCapacity(int capacity) {
		this.capacity = Math.max(1, capacity);
		if (energy > this.capacity) energy = this.capacity;
	}

	public void setMaxReceive(int maxReceive) {
		this.maxReceive = Math.max(0, maxReceive);
	}

	public int getMaxReceive() {
		return maxReceive;
	}

	public void addInternal(int amount) {
		energy = Math.min(capacity, energy + amount);
		onChanged.run();
	}

	public void removeInternal(int amount) {
		energy = Math.max(0, energy - amount);
		onChanged.run();
	}

	public void setEnergy(int amount) {
		energy = Math.max(0, Math.min(capacity, amount));
	}

	public int getSpace() {
		return capacity - energy;
	}
}
