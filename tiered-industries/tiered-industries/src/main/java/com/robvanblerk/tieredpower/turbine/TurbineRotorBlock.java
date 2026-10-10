package com.robvanblerk.tieredpower.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Turbine Rotor: a shaft section. Stack them inside an Industrial Turbine; the blades show (and spin) once it's formed. */
public class TurbineRotorBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

	public TurbineRotorBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
