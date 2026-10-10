package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/**
 * Wireless Access Point: put it anywhere on a storage network and Wireless Terminals work within 128 blocks of it.
 * Glows while the network is online (the Storage Controller keeps it updated).
 */
public class AccessPointBlock extends Block implements StorageNetworkBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 5, 14), Block.box(7, 5, 7, 9, 16, 9));

	public AccessPointBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return SHAPE;
	}
}
