package com.robvanblerk.tieredpower.energy;

/** Battery Box tiers. Capacity in FE, transfer rate in FE per tick (in and out, per face). */
public enum BatteryTier {
	BASIC("battery_box", 1_000_000, 8_000),
	ADVANCED("advanced_battery_box", 8_000_000, 32_000),
	ELITE("elite_battery_box", 32_000_000, 128_000),
	ULTIMATE("ultimate_battery_box", 256_000_000, 512_000),
	QUANTUM("quantum_battery_box", 2_000_000_000, 2_048_000);

	private final String id;
	private final int capacity;
	private final int rate;

	BatteryTier(String id, int capacity, int rate) {
		this.id = id;
		this.capacity = capacity;
		this.rate = rate;
	}

	public String getId() { return id; }
	/** Capacity and rate come from config/tieredpower-common.toml (defaults are the numbers above). */
	public int getCapacity() {
		return com.robvanblerk.tieredpower.Config.get(switch (this) {
			case BASIC -> com.robvanblerk.tieredpower.Config.BATTERY_BASIC_CAPACITY;
			case ADVANCED -> com.robvanblerk.tieredpower.Config.BATTERY_ADVANCED_CAPACITY;
			case ELITE -> com.robvanblerk.tieredpower.Config.BATTERY_ELITE_CAPACITY;
			case ULTIMATE -> com.robvanblerk.tieredpower.Config.BATTERY_ULTIMATE_CAPACITY;
			case QUANTUM -> com.robvanblerk.tieredpower.Config.BATTERY_QUANTUM_CAPACITY;
		});
	}

	public int getRate() {
		return com.robvanblerk.tieredpower.Config.get(switch (this) {
			case BASIC -> com.robvanblerk.tieredpower.Config.BATTERY_BASIC_RATE;
			case ADVANCED -> com.robvanblerk.tieredpower.Config.BATTERY_ADVANCED_RATE;
			case ELITE -> com.robvanblerk.tieredpower.Config.BATTERY_ELITE_RATE;
			case ULTIMATE -> com.robvanblerk.tieredpower.Config.BATTERY_ULTIMATE_RATE;
			case QUANTUM -> com.robvanblerk.tieredpower.Config.BATTERY_QUANTUM_RATE;
		});
	}
}
