package com.robvanblerk.tieredpower.stargate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

/**
 * Stargate addresses, dialled on the Dialler's DHD keypad: six glyphs that pick the destination, then the point of
 * origin (glyph 0), then the big red button. There are 38 glyphs; 1-37 make up addresses.
 */
public final class GateAddress {
	public static final int GLYPHS = 38, POINT_OF_ORIGIN = 0, LENGTH = 6;
	/** Home - the dimension you left from. */
	public static final int[] HOME = {27, 7, 15, 32, 12, 30};
	private static int[][] planets;

	private GateAddress() {}

	/** The six glyphs for a planet; fixed for every world. */
	public static int[] of(Planet p) {
		if (planets == null) {
			int[][] out = new int[Planet.values().length][];
			List<int[]> used = new ArrayList<>();
			used.add(HOME);
			for (Planet q : Planet.values()) {
				Random r = new Random(q.id.hashCode() * 31L + 0x5747);
				int[] a;
				do {
					List<Integer> pool = new ArrayList<>();
					for (int i = 1; i < GLYPHS; i++) pool.add(i);
					a = new int[LENGTH];
					for (int i = 0; i < LENGTH; i++) a[i] = pool.remove(r.nextInt(pool.size()));
				} while (contains(used, a));
				used.add(a);
				out[q.ordinal()] = a;
			}
			planets = out;
		}
		return planets[p.ordinal()];
	}

	private static boolean contains(List<int[]> list, int[] a) {
		for (int[] b : list) if (Arrays.equals(a, b)) return true;
		return false;
	}

	/** What a full address (six glyphs and the point of origin) dials: -1 home, a Planet ordinal, or null if nothing. */
	public static @Nullable Integer resolve(int[] entered) {
		if (entered.length != LENGTH + 1 || entered[LENGTH] != POINT_OF_ORIGIN) return null;
		int[] six = Arrays.copyOf(entered, LENGTH);
		if (Arrays.equals(six, HOME)) return -1;
		for (Planet p : Planet.values()) if (Arrays.equals(six, of(p))) return p.ordinal();
		return null;
	}
}
