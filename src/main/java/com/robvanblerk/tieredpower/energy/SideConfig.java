package com.robvanblerk.tieredpower.energy;

import net.minecraft.core.Direction;

/**
 * Per-face item settings for machines, relative to the machine's front (so rotating it with the Wrench keeps them).
 * Faces: 0 front, 1 back, 2 left, 3 right, 4 top, 5 bottom ("left/right" as you look at the front).
 * Modes: 0 off, 1 input only, 2 output only, 3 input + output (default).
 */
public final class SideConfig {
	public static final int OFF = 0, INPUT = 1, OUTPUT = 2, BOTH = 3;
	public static final String[] FACES = {"Front", "Back", "Left", "Right", "Top", "Bottom"};
	public static final String[] MODES = {"Off", "In", "Out", "In+Out"};

	private SideConfig() {}

	/** Which relative face (0-5) an absolute direction is, for a machine facing 'front'. */
	public static int faceIndex(Direction front, Direction dir) {
		if (dir == Direction.UP) return 4;
		if (dir == Direction.DOWN) return 5;
		if (dir == front) return 0;
		if (dir == front.getOpposite()) return 1;
		if (dir == front.getClockWise()) return 2;
		return 3;
	}

	public static boolean canInput(int mode) {
		return mode == INPUT || mode == BOTH;
	}

	public static boolean canOutput(int mode) {
		return mode == OUTPUT || mode == BOTH;
	}
}
