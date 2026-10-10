package com.robvanblerk.tieredpower.planet;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sporecap: a glowing mushroom that only grows on the Mycelia planet. Worth 2,000 mB of methane in a Bio-Digester. */
public class SporecapBlock extends BushBlock {
	private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 10, 12);

	public SporecapBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL) || state.is(BlockTags.DIRT) || state.is(BlockTags.NYLIUM);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
