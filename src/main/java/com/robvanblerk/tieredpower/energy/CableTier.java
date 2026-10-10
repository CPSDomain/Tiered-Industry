package com.robvanblerk.tieredpower.energy;

/**
 * The cable tiers. Each tier moves more Forge Energy (FE) per tick.
 * FE is the shared power unit on Forge (the same as RF), so these cables work with
 * Mekanism, Thermal, Immersive Engineering, Powah, Ender IO, AE2 and anything else that uses FE.
 * To add a tier: add a line here, then register a block for it in ModBlocks and add its assets.
 */
public enum CableTier {
	COPPER("copper", 1_000),
	GOLD("gold", 8_000),
	DIAMOND("diamond", 32_000),
	NETHERITE("netherite", 128_000),
	QUANTUM("quantum", 512_000);

	/** Rate from config/tieredpower-common.toml (defaults are the numbers above). */

	private final String name;
	private final int transferRate;

	CableTier(String name, int transferRate) {
		this.name = name;
		this.transferRate = transferRate;
	}

	public String getName() {
		return name;
	}

	/** Maximum FE this cable moves per tick. Also its internal buffer size. */
	public int getTransferRate() {
		return switch (this) {
			case COPPER -> com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.CABLE_COPPER);
			case GOLD -> com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.CABLE_GOLD);
			case DIAMOND -> com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.CABLE_DIAMOND);
			case NETHERITE -> com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.CABLE_NETHERITE);
			case QUANTUM -> com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.CABLE_QUANTUM);
		};
	}
}
