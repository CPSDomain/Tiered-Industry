package com.robvanblerk.tieredpower.energy;

import net.minecraft.util.StringRepresentable;

/** What a battery face does. Shift + right-click a face with an empty hand to cycle it. */
public enum SideMode implements StringRepresentable {
	INPUT("input"),
	OUTPUT("output"),
	DISABLED("disabled");

	private final String name;

	SideMode(String name) {
		this.name = name;
	}

	public SideMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public String displayName() {
		return switch (this) {
			case INPUT -> "Input";
			case OUTPUT -> "Output";
			case DISABLED -> "Disabled";
		};
	}
}
