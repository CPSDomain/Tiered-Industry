package com.robvanblerk.tieredpower.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Magnet Coil. The Fusion Controller switches ACTIVE on while the reactor runs: the coil lights up and animates. */
public class MagnetCoilBlock extends Block {
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public MagnetCoilBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}
}
