package com.robvanblerk.tieredpower.stargate;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Finding, forming and un-forming the 7x7 Stargate square. */
public final class StargateRing {
	public static final int SIZE = 7;

	/** A complete square: its lowest corner, which way it spans, its 24 frame blocks, inside blocks and master (bottom middle). */
	public record Shape(BlockPos corner, Direction.Axis axis, List<BlockPos> frame, List<BlockPos> inside, BlockPos master) {}

	private static boolean isFrame(Level level, BlockPos p) {
		return level.getBlockState(p).is(ModBlocks.STARGATE_FRAME.get());
	}

	/** Tries every square that could contain this frame block. */
	public static @Nullable Shape find(Level level, BlockPos frameBlock) {
		for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
			for (int a = 0; a < SIZE; a++) for (int y = 0; y < SIZE; y++) {
				BlockPos corner = axis == Direction.Axis.X ? frameBlock.offset(-a, -y, 0) : frameBlock.offset(0, -y, -a);
				Shape s = check(level, corner, axis);
				if (s != null) return s;
			}
		}
		return null;
	}

	public static @Nullable Shape check(Level level, BlockPos corner, Direction.Axis axis) {
		List<BlockPos> frame = new ArrayList<>(), inside = new ArrayList<>();
		for (int a = 0; a < SIZE; a++) for (int y = 0; y < SIZE; y++) {
			BlockPos p = axis == Direction.Axis.X ? corner.offset(a, y, 0) : corner.offset(0, y, a);
			boolean edge = a == 0 || a == SIZE - 1 || y == 0 || y == SIZE - 1;
			if (edge) {
				if (!isFrame(level, p)) return null;
				frame.add(p);
			} else {
				BlockState s = level.getBlockState(p);
				if (!s.isAir() && !s.is(ModBlocks.EVENT_HORIZON.get())) return null;
				inside.add(p);
			}
		}
		BlockPos master = axis == Direction.Axis.X ? corner.offset(SIZE / 2, 0, 0) : corner.offset(0, 0, SIZE / 2);
		return new Shape(corner, axis, frame, inside, master);
	}

	/** True if the round ring passes through this frame block (the four corner blocks of each corner are skipped). */
	private static boolean onRing(Shape s, BlockPos p) {
		int a = s.axis() == Direction.Axis.X ? p.getX() - s.corner().getX() : p.getZ() - s.corner().getZ();
		int y = p.getY() - s.corner().getY();
		double dx = a - 3, dy = y - 3;
		return Math.sqrt(dx * dx + dy * dy) < 3.9;
	}

	public static void form(Level level, Shape s, boolean formed) {
		for (BlockPos p : s.frame()) {
			BlockState st = level.getBlockState(p);
			if (!st.is(ModBlocks.STARGATE_FRAME.get())) continue;
			BlockState want = st.setValue(StargateFrameBlock.FORMED, formed).setValue(StargateFrameBlock.MASTER, formed && p.equals(s.master()))
					.setValue(StargateFrameBlock.RING, !formed || onRing(s, p)).setValue(StargateFrameBlock.AXIS, s.axis());
			if (want != st) level.setBlock(p, want, Block.UPDATE_ALL);
			if (!want.getValue(StargateFrameBlock.MASTER) && level.getBlockEntity(p) != null) level.removeBlockEntity(p);
		}
	}

	/** A formed frame was broken: turn the rest of its square back into plain blocks. */
	public static void unformAround(Level level, BlockPos broken) {
		for (int dx = -SIZE; dx <= SIZE; dx++) for (int dy = -SIZE; dy <= SIZE; dy++) for (int dz = -SIZE; dz <= SIZE; dz++) {
			BlockPos p = broken.offset(dx, dy, dz);
			BlockState st = level.getBlockState(p);
			if (st.is(ModBlocks.STARGATE_FRAME.get()) && st.getValue(StargateFrameBlock.FORMED)) {
				level.setBlock(p, st.setValue(StargateFrameBlock.FORMED, false).setValue(StargateFrameBlock.MASTER, false).setValue(StargateFrameBlock.RING, true), Block.UPDATE_ALL);
				if (level.getBlockEntity(p) != null) level.removeBlockEntity(p);
			}
			else if (st.is(ModBlocks.EVENT_HORIZON.get()) && st.getValue(EventHorizonBlock.HIDDEN))
				level.setBlock(p, st.setValue(EventHorizonBlock.HIDDEN, false), Block.UPDATE_ALL);
		}
	}

	/** The nearest complete square within 'radius' blocks of 'pos' (frames scanned nearest first), or null. */
	public static @Nullable Shape near(Level level, BlockPos pos, int radius) {
		for (int r = 0; r <= radius; r++)
			for (int dx = -r; dx <= r; dx++) for (int dy = -r; dy <= r; dy++) for (int dz = -r; dz <= r; dz++) {
				if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r) continue;
				BlockPos p = pos.offset(dx, dy, dz);
				if (!isFrame(level, p)) continue;
				Shape s = find(level, p);
				if (s != null) return s;
			}
		return null;
	}

	/** The point 'dist' blocks out from the middle of the ring on side 'side' (+1/-1 along its normal), at floor level. */
	public static BlockPos front(Shape s, int side, int dist) {
		return s.axis() == Direction.Axis.X ? s.master().offset(0, 1, side * dist) : s.master().offset(side * dist, 1, 0);
	}

	public static boolean isFormed(Level level, Shape s) {
		BlockState m = level.getBlockState(s.master());
		return m.is(ModBlocks.STARGATE_FRAME.get()) && m.getValue(StargateFrameBlock.MASTER);
	}

	private StargateRing() {}
}
