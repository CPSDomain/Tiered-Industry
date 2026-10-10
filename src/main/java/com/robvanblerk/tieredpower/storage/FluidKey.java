package com.robvanblerk.tieredpower.storage;

import java.util.Objects;

import net.minecraftforge.fluids.FluidStack;

/** A fluid or gas type (fluid + NBT), usable as a map key. Holds a copy with amount 1. */
public final class FluidKey {
	private final FluidStack proto;
	private final int hash;

	public FluidKey(FluidStack stack) {
		this.proto = new FluidStack(stack, 1);
		this.hash = Objects.hash(proto.getFluid(), proto.getTag());
	}

	public FluidStack proto() {
		return proto;
	}

	public FluidStack toStack(int amount) {
		return new FluidStack(proto, amount);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof FluidKey k && proto.isFluidEqual(k.proto);
	}

	@Override
	public int hashCode() {
		return hash;
	}
}
