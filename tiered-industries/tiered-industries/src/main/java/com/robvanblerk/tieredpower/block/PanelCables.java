package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Panels (terminal panels, Assembler Panel, Machine Connector) remember which sides have Storage Cable next to them, so
 * their model can draw a short cable piece from that side to the plate - otherwise there's a visible gap between the
 * cable and the thin panel. Same property names as cables (north, east, ...).
 */
public final class PanelCables {
	public static void addProperties(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CableBlock.CONNECTIONS.values().toArray(new BooleanProperty[0]));
	}

	public static BlockState none(BlockState state) {
		for (BooleanProperty p : CableBlock.CONNECTIONS.values()) state = state.setValue(p, false);
		return state;
	}

	private static boolean cableAt(LevelAccessor level, BlockPos pos) {
		return level.getBlockState(pos).getBlock() instanceof StorageCableBlock;
	}

	/** Sets every side (except the one the panel is stuck to). */
	public static BlockState update(BlockState state, LevelAccessor level, BlockPos pos) {
		return update(state, level, pos, state.getValue(DirectionalBlock.FACING).getOpposite());
	}

	/** One side changed. */
	public static BlockState update(BlockState state, Direction dir, BlockState neighbour) {
		return update(state, dir, neighbour, state.getValue(DirectionalBlock.FACING).getOpposite());
	}

	/** As above, for panels whose FACING points at the block they're stuck to (the buses): 'back' is that side. */
	public static BlockState update(BlockState state, LevelAccessor level, BlockPos pos, Direction back) {
		for (Direction d : Direction.values()) state = state.setValue(CableBlock.CONNECTIONS.get(d), d != back && cableAt(level, pos.relative(d)));
		return state;
	}

	public static BlockState update(BlockState state, Direction dir, BlockState neighbour, Direction back) {
		return state.setValue(CableBlock.CONNECTIONS.get(dir), dir != back && neighbour.getBlock() instanceof StorageCableBlock);
	}

	private PanelCables() {}
}
