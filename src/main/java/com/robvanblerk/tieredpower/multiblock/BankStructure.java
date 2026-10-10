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

import com.robvanblerk.tieredpower.block.EnergyCellBlock;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Checks the Energy Bank multiblock. Same rules as the Fusion Reactor: any rectangular box from 3x3x3 to 13x13x13.
 *   inside        -> Energy Cells (any tier) or air
 *   walls         -> Bank Casing, Bank Glass or Bank Ports (plus the controller)
 *   edges/corners -> Bank Casing only
 */
public final class BankStructure {
	public static final int MAX_INTERIOR = 11;

	public record Result(boolean formed, long capacity, int cells, List<BlockPos> ports, String size, Component message) {}

	public static Result check(Level level, BlockPos controllerPos, Direction facing) {
		BlockPos start = controllerPos.relative(facing.getOpposite());
		BlockState startState = level.getBlockState(start);
		if (!isInterior(startState)) {
			return fail("an Energy Cell or air directly behind the controller (the controller must be built into a wall)", start, startState);
		}

		int minX = start.getX() - reach(level, start, Direction.WEST);
		int maxX = start.getX() + reach(level, start, Direction.EAST);
		int minY = start.getY() - reach(level, start, Direction.DOWN);
		int maxY = start.getY() + reach(level, start, Direction.UP);
		int minZ = start.getZ() - reach(level, start, Direction.NORTH);
		int maxZ = start.getZ() + reach(level, start, Direction.SOUTH);
		int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;
		if (sizeX > MAX_INTERIOR || sizeY > MAX_INTERIOR || sizeZ > MAX_INTERIOR) {
			return new Result(false, 0, 0, List.of(), "", Component.literal("Energy Bank is too big (or has a hole in its walls): the inside can be at most "
					+ MAX_INTERIOR + " blocks in each direction. Measured inside: " + sizeX + " x " + sizeY + " x " + sizeZ + "."));
		}

		List<BlockPos> ports = new ArrayList<>();
		long capacity = 0;
		int cells = 0;

		for (int x = minX - 1; x <= maxX + 1; x++) {
			for (int y = minY - 1; y <= maxY + 1; y++) {
				for (int z = minZ - 1; z <= maxZ + 1; z++) {
					BlockPos pos = new BlockPos(x, y, z);
					int outside = (x < minX || x > maxX ? 1 : 0) + (y < minY || y > maxY ? 1 : 0) + (z < minZ || z > maxZ ? 1 : 0);
					BlockState state = level.getBlockState(pos);

					if (pos.equals(controllerPos)) {
						if (outside != 1) return fail("the Bank Controller to be on a wall, not an edge or corner", pos, state);
						continue;
					}

					if (outside == 0) {
						if (state.getBlock() instanceof EnergyCellBlock cell) {
							capacity += cell.getCapacity();
							cells++;
						} else if (!state.isAir()) {
							return fail("an Energy Cell or air (inside the bank)", pos, state);
						}
					} else if (outside == 1) {
						if (state.is(ModBlocks.BANK_PORT.get())) ports.add(pos);
						else if (!state.is(ModBlocks.BANK_CASING.get()) && !state.is(ModBlocks.BANK_GLASS.get())) {
							return fail("Bank Casing, Bank Glass or a Bank Port (wall of the bank)", pos, state);
						}
					} else if (!state.is(ModBlocks.BANK_CASING.get())) {
						return fail("Bank Casing (edges and corners)", pos, state);
					}
				}
			}
		}

		String size = (sizeX + 2) + "x" + (sizeY + 2) + "x" + (sizeZ + 2);
		if (ports.isEmpty()) {
			return new Result(false, capacity, cells, ports, size, Component.literal("Energy Bank (" + size + ") needs at least one Bank Port built into a wall."));
		}
		return new Result(true, capacity, cells, ports, size, Component.literal("Energy Bank formed: " + size + " with " + cells + " cells."));
	}

	private static int reach(Level level, BlockPos start, Direction dir) {
		int n = 0;
		while (n <= MAX_INTERIOR && isInterior(level.getBlockState(start.relative(dir, n + 1)))) n++;
		return n;
	}

	private static boolean isInterior(BlockState state) {
		return state.isAir() || state.getBlock() instanceof EnergyCellBlock;
	}

	private static Result fail(String expected, BlockPos pos, BlockState found) {
		Component foundName = found.isAir() ? Component.literal("nothing (air)") : found.getBlock().getName();
		MutableComponent message = Component.literal("Energy Bank incomplete at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
				+ ": expected " + expected + ", but found ").append(foundName).append(".");
		return new Result(false, 0, 0, List.of(), "", message);
	}

	/** Bank blocks (ports don't push power back into these). */
	public static boolean isBankPart(Block block) {
		return block == ModBlocks.BANK_CASING.get() || block == ModBlocks.BANK_GLASS.get() || block == ModBlocks.BANK_PORT.get()
				|| block == ModBlocks.BANK_CONTROLLER.get() || block instanceof EnergyCellBlock;
	}

	private BankStructure() {}
}
