package com.robvanblerk.tieredpower.energy;

import net.minecraft.util.StringRepresentable;

/**
 * One side of a pipe:
 *   NONE     - nothing there to connect to
 *   PIPE     - "Push": delivers into the neighbouring block (the default)
 *   EXTRACT  - "Pull": takes out of the neighbouring block
 *   BOTH     - "Push + Pull"
 *   DISABLED - there's something there, but this side is switched off
 * Cycle PIPE -> EXTRACT -> BOTH -> DISABLED with the Wrench.
 */
public enum PipeSide implements StringRepresentable {
	NONE("none"),
	PIPE("pipe"),
	EXTRACT("extract"),
	BOTH("both"),
	DISABLED("disabled");

	private final String name;

	PipeSide(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public boolean pushes() {
		return this == PIPE || this == BOTH;
	}

	public boolean pulls() {
		return this == EXTRACT || this == BOTH;
	}

	/** Keeps the player's choice while the neighbour is still there. */
	public boolean isChoice() {
		return this == EXTRACT || this == BOTH || this == DISABLED;
	}

	public PipeSide nextMode() {
		return switch (this) {
			case PIPE -> EXTRACT;
			case EXTRACT -> BOTH;
			case BOTH -> DISABLED;
			default -> PIPE;
		};
	}

	public String describe() {
		return switch (this) {
			case PIPE -> "Push (delivers INTO that block)";
			case EXTRACT -> "Pull (takes OUT of that block)";
			case BOTH -> "Push + Pull";
			case DISABLED -> "Disabled (not connected)";
			default -> "Nothing connected";
		};
	}
}
