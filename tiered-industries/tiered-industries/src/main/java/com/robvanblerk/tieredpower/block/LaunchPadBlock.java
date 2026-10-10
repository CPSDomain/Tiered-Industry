package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Launch Pad deck plate (4 pixels high) and the Launch Tower lattice: the cosmetic parts of the launch site. */
public class LaunchPadBlock extends Block {
	private static final VoxelShape DECK = Block.box(0, 0, 0, 16, 4, 16);
	private final boolean tower;

	public LaunchPadBlock(boolean tower, Properties properties) {
		super(properties);
		this.tower = tower;
	}

	public boolean isTower() {
		return tower;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return tower ? super.getShape(state, level, pos, ctx) : DECK;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
