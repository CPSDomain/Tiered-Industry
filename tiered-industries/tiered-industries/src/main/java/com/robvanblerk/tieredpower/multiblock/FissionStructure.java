package com.robvanblerk.tieredpower.multiblock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Checks the multiblock Fission Reactor: any rectangular box from 3x3x3 to 13x13x13.
 *   inside        -> Fuel Assemblies, Coolant Channels, water or air (the controller floods the air with water from its Coolant Ports)
 *   walls         -> Fission Casing, Fission Glass, Reactor Gauges, Fuel / Coolant / Power Ports (plus the controller)
 *   edges/corners -> Fission Casing only
 * Also counts how many Fuel Assemblies touch each other (touching assemblies run more efficiently).
 */
public final class FissionStructure {
	public static final int MAX_INTERIOR = 11;

	public record Result(boolean formed, int assemblies, int channels, int neighbourPairs, List<BlockPos> ports,
			List<BlockPos> assemblyPositions, String size, Component message, int waterBlocks, List<BlockPos> airPositions) {
		public Result(boolean formed, int assemblies, int channels, int neighbourPairs, List<BlockPos> ports,
				List<BlockPos> assemblyPositions, String size, Component message) {
			this(formed, assemblies, channels, neighbourPairs, ports, assemblyPositions, size, message, 0, List.of());
		}
	}

	public static Result check(Level level, BlockPos controllerPos, Direction facing) {
		BlockPos start = controllerPos.relative(facing.getOpposite());
		BlockState startState = level.getBlockState(start);
		if (!isInterior(startState)) {
			return fail("a Fuel Assembly, Coolant Channel or air directly behind the controller (the controller must be built into a wall)", start, startState);
		}
		int minX = start.getX() - reach(level, start, Direction.WEST), maxX = start.getX() + reach(level, start, Direction.EAST);
		int minY = start.getY() - reach(level, start, Direction.DOWN), maxY = start.getY() + reach(level, start, Direction.UP);
		int minZ = start.getZ() - reach(level, start, Direction.NORTH), maxZ = start.getZ() + reach(level, start, Direction.SOUTH);
		int sx = maxX - minX + 1, sy = maxY - minY + 1, sz = maxZ - minZ + 1;
		if (sx > MAX_INTERIOR || sy > MAX_INTERIOR || sz > MAX_INTERIOR) {
			return new Result(false, 0, 0, 0, List.of(), List.of(), "", Component.literal("Fission Reactor is too big (or has a hole in its walls): the inside can be at most "
					+ MAX_INTERIOR + " blocks in each direction. Measured inside: " + sx + " x " + sy + " x " + sz + "."));
		}

		List<BlockPos> ports = new ArrayList<>();
		List<BlockPos> assemblies = new ArrayList<>();
		int channels = 0, fuelPorts = 0, coolantPorts = 0, powerPorts = 0, water = 0;
		List<BlockPos> air = new ArrayList<>();
		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					int outside = (x < minX || x > maxX ? 1 : 0) + (y < minY || y > maxY ? 1 : 0) + (z < minZ || z > maxZ ? 1 : 0);
					BlockState state = level.getBlockState(pos);
					if (pos.equals(controllerPos)) {
						if (outside != 1) return fail("the Fission Controller to be on a wall, not an edge or corner", pos, state);
						continue;
					}
					if (outside == 0) {
						if (state.is(ModBlocks.FUEL_ASSEMBLY.get())) assemblies.add(pos);
						else if (state.is(ModBlocks.COOLANT_CHANNEL.get())) channels++;
						else if (state.is(ModBlocks.CRYONITE_BLOCK.get())) channels += 3; // planet ice: three channels' worth
						else if (state.is(Blocks.WATER)) water++;
						else if (state.isAir()) air.add(pos);
						else return fail("a Fuel Assembly, Coolant Channel, water or air (inside the reactor)", pos, state);
					} else if (outside == 1) {
						if (state.is(ModBlocks.FISSION_FUEL_PORT.get())) { fuelPorts++; ports.add(pos); }
						else if (state.is(ModBlocks.FISSION_COOLANT_PORT.get())) { coolantPorts++; ports.add(pos); }
						else if (state.is(ModBlocks.FISSION_POWER_PORT.get())) { powerPorts++; ports.add(pos); }
						else if (state.is(ModBlocks.FISSION_WASTE_PORT.get())) ports.add(pos);
						else if (state.is(ModBlocks.REACTOR_GAUGE.get())) ports.add(pos); // a gauge can stand in for a wall block
						else if (!state.is(ModBlocks.FISSION_CASING.get()) && !state.is(ModBlocks.FISSION_GLASS.get())) {
							return fail("Fission Casing, Fission Glass, a Reactor Gauge or a Fuel / Waste / Coolant / Power Port (wall of the reactor)", pos, state);
						}
					} else if (!state.is(ModBlocks.FISSION_CASING.get())) {
						return fail("Fission Casing (edges and corners)", pos, state);
					}
				}
			}
		}

		// Count touching pairs of fuel assemblies.
		Set<BlockPos> set = new HashSet<>(assemblies);
		int pairs = 0;
		for (BlockPos p : assemblies) {
			for (Direction d : new Direction[]{Direction.EAST, Direction.UP, Direction.SOUTH}) if (set.contains(p.relative(d))) pairs++;
		}

		String size = (sx + 2) + "x" + (sy + 2) + "x" + (sz + 2);
		String missing = assemblies.isEmpty() ? "at least one Fuel Assembly inside" : fuelPorts == 0 ? "a Fuel Port" : coolantPorts == 0 ? "a Coolant Port"
				: powerPorts == 0 ? "a Power Port" : null;
		if (missing != null) {
			return new Result(false, assemblies.size(), channels, pairs, ports, assemblies, size,
					Component.literal("Fission Reactor (" + size + ") needs " + missing + "."));
		}
		// Fill from the bottom up so the water looks like it's rising.
		air.sort(java.util.Comparator.comparingInt(BlockPos::getY));
		return new Result(true, assemblies.size(), channels, pairs, ports, assemblies, size,
				Component.literal("Fission Reactor formed: " + size + ", " + assemblies.size() + " fuel assemblies, " + channels + " coolant channels."),
				water, air);
	}

	private static int reach(Level level, BlockPos start, Direction dir) {
		int n = 0;
		while (n <= MAX_INTERIOR && isInterior(level.getBlockState(start.relative(dir, n + 1)))) n++;
		return n;
	}

	private static boolean isInterior(BlockState state) {
		return state.isAir() || state.is(Blocks.WATER) || state.is(ModBlocks.FUEL_ASSEMBLY.get()) || state.is(ModBlocks.COOLANT_CHANNEL.get())
				|| state.is(ModBlocks.CRYONITE_BLOCK.get());
	}

	private static Result fail(String expected, BlockPos pos, BlockState found) {
		Component foundName = found.isAir() ? Component.literal("nothing (air)") : found.getBlock().getName();
		MutableComponent message = Component.literal("Fission Reactor incomplete at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
				+ ": expected " + expected + ", but found ").append(foundName).append(".");
		return new Result(false, 0, 0, 0, List.of(), List.of(), "", message);
	}

	public static boolean isPart(Block block) {
		return block == ModBlocks.FISSION_CASING.get() || block == ModBlocks.FISSION_GLASS.get() || block == ModBlocks.FISSION_FUEL_PORT.get()
				|| block == ModBlocks.FISSION_COOLANT_PORT.get() || block == ModBlocks.FISSION_POWER_PORT.get() || block == ModBlocks.FISSION_WASTE_PORT.get()
				|| block == ModBlocks.FISSION_CONTROLLER.get() || block == ModBlocks.FUEL_ASSEMBLY.get() || block == ModBlocks.COOLANT_CHANNEL.get() || block == ModBlocks.CRYONITE_BLOCK.get()
				|| block == ModBlocks.REACTOR_GAUGE.get();
	}

	private FissionStructure() {}
}
