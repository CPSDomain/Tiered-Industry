package com.robvanblerk.tieredpower.spatial;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class SpatialProjectorBlock extends MachineBlock {
	/** Which cell is in it (0 none, 1 small, 2 medium, 3 large) - so clients can draw the outline of the area. */
	public static final IntegerProperty SIZE = IntegerProperty.create("size", 0, 3);

	public SpatialProjectorBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SIZE);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SpatialProjectorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SPATIAL_PROJECTOR.get(), SpatialProjectorBlockEntity::tick);
	}
}
