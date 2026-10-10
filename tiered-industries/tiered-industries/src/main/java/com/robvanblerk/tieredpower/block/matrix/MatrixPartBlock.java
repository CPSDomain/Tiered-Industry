package com.robvanblerk.tieredpower.block.matrix;

import net.minecraft.world.level.block.Block;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Plain Assembly Matrix parts (Casing, Glass, Crafting Accelerator) and the Machine Connector: storage-network blocks. */
public class MatrixPartBlock extends Block implements StorageNetworkBlock {
	public enum Kind { CASING, GLASS, ACCELERATOR, CONNECTOR }

	private final Kind kind;

	public MatrixPartBlock(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

	// Glass: let light through and don't darken neighbours.
	@Override
	@SuppressWarnings("deprecation")
	public float getShadeBrightness(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		return kind == Kind.GLASS ? 1.0f : super.getShadeBrightness(state, level, pos);
	}

	@Override
	public boolean propagatesSkylightDown(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		return kind == Kind.GLASS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean skipRendering(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.state.BlockState adjacent, net.minecraft.core.Direction side) {
		return kind == Kind.GLASS && adjacent.is(this) || super.skipRendering(state, adjacent, side);
	}
}
