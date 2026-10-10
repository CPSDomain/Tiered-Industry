package com.robvanblerk.tieredpower.energy;

import net.minecraftforge.fluids.FluidStack;

/** Fluid and Gas Pipe tiers. Rates are mB per tick per connection. */
public enum PipeTier {
	BASIC("basic", 1_000),
	ADVANCED("advanced", 4_000),
	ELITE("elite", 16_000),
	ULTIMATE("ultimate", 64_000),
	QUANTUM("quantum", 256_000);

	public enum Kind {
		/** Liquids: water, lava, and other mods' liquids. */
		LIQUID("fluid_pipe"),
		/** Gases: fluids lighter than air, such as Steam. */
		GAS("gas_pipe");

		public final String suffix;

		Kind(String suffix) {
			this.suffix = suffix;
		}

		public boolean accepts(FluidStack stack) {
			if (stack.isEmpty()) return false;
			boolean gas = stack.getFluid().getFluidType().isLighterThanAir();
			return this == GAS ? gas : !gas;
		}
	}

	private final String name;
	private final int rate;

	PipeTier(String name, int rate) {
		this.name = name;
		this.rate = rate;
	}

	public String getName() {
		return name;
	}

	public int getRate() {
		return rate;
	}
}
