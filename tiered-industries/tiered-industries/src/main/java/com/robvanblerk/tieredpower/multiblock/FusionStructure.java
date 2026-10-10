package com.robvanblerk.tieredpower.multiblock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Checks the multiblock Fusion Reactor. Any rectangular box works, from 3x3x3 up to 13x13x13 (outside size).
 *
 * The controller is built into one side wall, front facing out. The block directly behind it is inside the
 * reactor; from there the checker measures how far the inside (Magnet Coils or air) goes in every direction,
 * which gives the interior box. It then checks the whole shell:
 *   inside       -> Magnet Coil or air (every coil adds output)
 *   faces        -> Casing, Glass, Fuel Port, Power Port (or the controller)
 *   edges/corners-> Casing only
 */
public final class FusionStructure {
	/** Largest inside size in each direction (outside = inside + 2). */
	public static final int MAX_INTERIOR = 11;

	public record Result(boolean formed, int coils, List<BlockPos> ports, Component message, List<BlockPos> coilPositions) {
		public Result(boolean formed, int coils, List<BlockPos> ports, Component message) {
			this(formed, coils, ports, message, List.of());
		}
	}

	public static Result check(Level level, BlockPos controllerPos, Direction facing) {
		BlockPos start = controllerPos.relative(facing.getOpposite());
		BlockState startState = level.getBlockState(start);
		if (!isInterior(startState)) {
			return fail("Magnet Coil or air directly behind the controller (the controller must be built into a wall)",
					start, startState);
		}

		// Measure the inside by walking out from the starting block along each axis.
		int minX = start.getX() - reach(level, start, Direction.WEST);
		int maxX = start.getX() + reach(level, start, Direction.EAST);
		int minY = start.getY() - reach(level, start, Direction.DOWN);
		int maxY = start.getY() + reach(level, start, Direction.UP);
		int minZ = start.getZ() - reach(level, start, Direction.NORTH);
		int maxZ = start.getZ() + reach(level, start, Direction.SOUTH);

		int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;
		if (sizeX > MAX_INTERIOR || sizeY > MAX_INTERIOR || sizeZ > MAX_INTERIOR) {
			return new Result(false, 0, List.of(), Component.literal("Fusion Reactor is too big (or has a hole in its walls): "
					+ "the inside can be at most " + MAX_INTERIOR + " blocks in each direction. Measured inside: "
					+ sizeX + " x " + sizeY + " x " + sizeZ + "."));
		}

		List<BlockPos> ports = new ArrayList<>();
		int coils = 0, fuelPorts = 0, powerPorts = 0;
		List<BlockPos> coilPositions = new ArrayList<>();

		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					int outside = (x < minX || x > maxX ? 1 : 0) + (y < minY || y > maxY ? 1 : 0) + (z < minZ || z > maxZ ? 1 : 0);
					BlockState state = level.getBlockState(pos);

					if (pos.equals(controllerPos)) {
						if (outside != 1) return fail("the Fusion Controller to be on a wall, not an edge or corner", pos, state);
						continue;
					}

					if (outside == 0) {
						if (state.is(ModBlocks.MAGNET_COIL.get())) { coils++; coilPositions.add(pos); }
						else if (!state.isAir()) return fail("Magnet Coil or air (inside the reactor)", pos, state);
					} else if (outside == 1) {
						if (state.is(ModBlocks.REACTOR_FUEL_PORT.get())) { fuelPorts++; ports.add(pos); }
						else if (state.is(ModBlocks.REACTOR_POWER_PORT.get())) { powerPorts++; ports.add(pos); }
						else if (state.is(ModBlocks.REACTOR_GAUGE.get())) ports.add(pos); // a gauge can stand in for a wall block
						else if (!state.is(ModBlocks.REACTOR_CASING.get()) && !state.is(ModBlocks.REACTOR_GLASS.get())) {
							return fail("Reactor Casing, Glass, a Reactor Gauge or a Port (wall of the reactor)", pos, state);
						}
					} else if (!state.is(ModBlocks.REACTOR_CASING.get())) {
						return fail("Reactor Casing (edges and corners)", pos, state);
					}
				}
			}
		}

		String size = (sizeX + 2) + "x" + (sizeY + 2) + "x" + (sizeZ + 2);
		if (fuelPorts == 0) return new Result(false, coils, ports,
				Component.literal("Fusion Reactor (" + size + ") needs at least one Fuel Port built into a wall."));
		if (powerPorts == 0) return new Result(false, coils, ports,
				Component.literal("Fusion Reactor (" + size + ") needs at least one Power Port built into a wall."));
		return new Result(true, coils, ports, Component.literal("Fusion Reactor formed: " + size + " with " + coils
				+ " Magnet Coil" + (coils == 1 ? "" : "s") + "."), coilPositions);
	}

	/** How many interior blocks there are beyond start in this direction (stops at the wall, or just past the max size). */
	private static int reach(Level level, BlockPos start, Direction dir) {
		int n = 0;
		while (n <= MAX_INTERIOR && isInterior(level.getBlockState(start.relative(dir, n + 1)))) n++;
		return n;
	}

	private static boolean isInterior(BlockState state) {
		return state.isAir() || state.is(ModBlocks.MAGNET_COIL.get());
	}

	private static Result fail(String expected, BlockPos pos, BlockState found) {
		Component foundName = found.isAir() ? Component.literal("nothing (air)") : found.getBlock().getName();
		MutableComponent message = Component.literal("Fusion Reactor incomplete at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
				+ ": expected " + expected + ", but found ").append(foundName).append(".");
		return new Result(false, 0, List.of(), message);
	}

	/** Is this block one of the reactor's own parts (so power ports don't push power back into the reactor)? */
	public static boolean isReactorPart(Block block) {
		return block == ModBlocks.REACTOR_CASING.get() || block == ModBlocks.REACTOR_GLASS.get()
				|| block == ModBlocks.REACTOR_FUEL_PORT.get() || block == ModBlocks.REACTOR_POWER_PORT.get()
				|| block == ModBlocks.MAGNET_COIL.get() || block == ModBlocks.FUSION_CONTROLLER.get() || block == ModBlocks.REACTOR_GAUGE.get();
	}

	private FusionStructure() {}
}
