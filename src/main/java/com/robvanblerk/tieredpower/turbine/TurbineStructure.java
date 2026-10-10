package com.robvanblerk.tieredpower.turbine;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Checks the Industrial Turbine: any rectangular box from 3x3x3 up to 13x13x13 (outside).
 *   inside        -> Turbine Rotors or air
 *   walls         -> Turbine Casing, Turbine Glass, Turbine Valves (plus the controller)
 *   edges/corners -> Turbine Casing only
 * Needs at least one rotor and one valve.
 */
public final class TurbineStructure {
	public static final int MAX_INTERIOR = 11;

	public record Result(boolean formed, List<BlockPos> rotors, List<BlockPos> valves, int volume, String size, Component message) {}

	public static Result check(Level level, BlockPos controllerPos, Direction facing) {
		BlockPos start = controllerPos.relative(facing.getOpposite());
		if (!isInterior(level.getBlockState(start)))
			return fail("a Turbine Rotor or air directly behind the controller (build the controller into a wall)", start, level.getBlockState(start));
		int minX = start.getX() - reach(level, start, Direction.WEST), maxX = start.getX() + reach(level, start, Direction.EAST);
		int minY = start.getY() - reach(level, start, Direction.DOWN), maxY = start.getY() + reach(level, start, Direction.UP);
		int minZ = start.getZ() - reach(level, start, Direction.NORTH), maxZ = start.getZ() + reach(level, start, Direction.SOUTH);
		int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
		if (sx > MAX_INTERIOR || sy > MAX_INTERIOR || sz > MAX_INTERIOR)
			return new Result(false, List.of(), List.of(), 0, "", Component.literal("Industrial Turbine too big (or a hole in a wall): the inside can be at most "
					+ MAX_INTERIOR + " blocks each way. Measured inside: " + sx + " x " + sy + " x " + sz + "."));

		List<BlockPos> rotors = new ArrayList<>(), valves = new ArrayList<>();
		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					BlockPos p = new BlockPos(x, y, z);
					int outside = (x < minX || x > maxX ? 1 : 0) + (y < minY || y > maxY ? 1 : 0) + (z < minZ || z > maxZ ? 1 : 0);
					BlockState s = level.getBlockState(p);
					if (p.equals(controllerPos)) {
						if (outside != 1) return fail("the Turbine Controller on a wall, not an edge or corner", p, s);
						continue;
					}
					if (outside == 0) {
						if (s.is(ModBlocks.TURBINE_ROTOR.get())) rotors.add(p);
						else if (!s.isAir()) return fail("a Turbine Rotor or air (inside the turbine)", p, s);
					} else if (outside == 1) {
						if (s.is(ModBlocks.TURBINE_VALVE.get())) valves.add(p);
						else if (!s.is(ModBlocks.TURBINE_CASING.get()) && !s.is(ModBlocks.TURBINE_GLASS.get()))
							return fail("Turbine Casing, Turbine Glass or a Turbine Valve (wall)", p, s);
					} else if (!s.is(ModBlocks.TURBINE_CASING.get())) {
						return fail("Turbine Casing (edges and corners)", p, s);
					}
				}
			}
		}
		String size = (sx + 2) + "x" + (sy + 2) + "x" + (sz + 2);
		if (rotors.isEmpty()) return new Result(false, rotors, valves, 0, size, Component.literal("Industrial Turbine (" + size + ") needs at least one Turbine Rotor inside."));
		if (valves.isEmpty()) return new Result(false, rotors, valves, 0, size, Component.literal("Industrial Turbine (" + size + ") needs at least one Turbine Valve in a wall."));
		return new Result(true, rotors, valves, sx * sy * sz, size, Component.literal("Industrial Turbine formed: " + size + " with " + rotors.size() + " rotors."));
	}

	private static int reach(Level level, BlockPos start, Direction dir) {
		int n = 0;
		while (n <= MAX_INTERIOR && isInterior(level.getBlockState(start.relative(dir, n + 1)))) n++;
		return n;
	}

	private static boolean isInterior(BlockState s) {
		return s.isAir() || s.is(ModBlocks.TURBINE_ROTOR.get());
	}

	private static Result fail(String expected, BlockPos p, BlockState found) {
		Component name = found.isAir() ? Component.literal("nothing (air)") : found.getBlock().getName();
		return new Result(false, List.of(), List.of(), 0, "", Component.literal("Industrial Turbine incomplete at " + p.getX() + ", " + p.getY() + ", " + p.getZ()
				+ ": expected " + expected + ", but found ").append(name).append("."));
	}

	public static boolean isPart(Block b) {
		return b == ModBlocks.TURBINE_CASING.get() || b == ModBlocks.TURBINE_GLASS.get() || b == ModBlocks.TURBINE_VALVE.get()
				|| b == ModBlocks.TURBINE_CONTROLLER.get() || b == ModBlocks.TURBINE_ROTOR.get();
	}

	private TurbineStructure() {}
}
