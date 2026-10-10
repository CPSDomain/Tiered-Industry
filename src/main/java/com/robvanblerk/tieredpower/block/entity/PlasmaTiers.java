package com.robvanblerk.tieredpower.block.entity;

/**
 * Fusion plasma tiers, raised with Plasma Coils (Mk I-III, fitted in order). Each tier multiplies the reactor's output
 * and burns fuel faster - but always less than the output gain, so every tier gets more power from each fuel pair.
 */
public final class PlasmaTiers {
	public static final String[] NAMES = {"Standard", "Mk I", "Mk II", "Mk III"};
	/** Output x1, x2, x3, x4. */
	public static final int[] OUTPUT = {1, 2, 3, 4};
	/** Fuel burns x1, x1.5, x2, x2.5 as fast (in halves, to stay in whole numbers). */
	public static final int[] BURN_HALVES = {2, 3, 4, 5};

	public static int burnTicks(int base, int tier) {
		return Math.max(20, base * 2 / BURN_HALVES[clamp(tier)]);
	}

	public static int output(int base, int tier) {
		return (int) Math.min(Integer.MAX_VALUE, (long) base * OUTPUT[clamp(tier)]);
	}

	public static int clamp(int tier) {
		return Math.max(0, Math.min(3, tier));
	}

	private PlasmaTiers() {}
}
