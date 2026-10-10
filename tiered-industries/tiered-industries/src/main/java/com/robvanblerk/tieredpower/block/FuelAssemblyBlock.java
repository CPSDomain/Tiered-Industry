package com.robvanblerk.tieredpower.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Goes inside the multiblock Fission Reactor. Glows and pulses (ACTIVE) while the reactor is burning fuel. */
public class FuelAssemblyBlock extends Block {
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	public FuelAssemblyBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ACTIVE);
	}
}
