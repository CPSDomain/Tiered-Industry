package com.robvanblerk.tieredpower.energy;

/** How a machine responds to redstone. Cycled with the button in the top-right of its GUI. */
public enum RedstoneMode {
	ALWAYS("Ignore redstone"),
	HIGH("Run only with a redstone signal"),
	LOW("Run only without a redstone signal");

	private final String description;

	RedstoneMode(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public RedstoneMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public boolean allows(boolean powered) {
		return switch (this) {
			case ALWAYS -> true;
			case HIGH -> powered;
			case LOW -> !powered;
		};
	}

	public static RedstoneMode byId(int id) {
		RedstoneMode[] all = values();
		return id >= 0 && id < all.length ? all[id] : ALWAYS;
	}
}
