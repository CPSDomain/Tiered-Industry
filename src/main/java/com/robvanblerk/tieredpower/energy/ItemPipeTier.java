package com.robvanblerk.tieredpower.energy;

/** Item Pipe tiers: items moved per second by each PULL connection. Deliveries from machines pushing in are not limited. */
public enum ItemPipeTier {
	BASIC("basic", 8),
	ADVANCED("advanced", 32),
	ELITE("elite", 128),
	ULTIMATE("ultimate", 512),
	QUANTUM("quantum", 2_048);

	private final String name;
	private final int itemsPerSecond;

	ItemPipeTier(String name, int itemsPerSecond) {
		this.name = name;
		this.itemsPerSecond = itemsPerSecond;
	}

	public String getName() {
		return name;
	}

	public int getItemsPerSecond() {
		return itemsPerSecond;
	}
}
