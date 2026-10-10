package com.robvanblerk.tieredpower.stargate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class StargateDialerBlock extends MachineBlock {
	/** Shaped like a DHD: a pedestal under a wide, sloping keypad console. */
	private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
			Block.box(2, 0, 2, 14, 3, 14), Block.box(4, 3, 4, 12, 10, 12), Block.box(1, 9, 1, 15, 15, 15));

	public StargateDialerBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof StargateDialerBlockEntity d && d.isGateOpen()) d.close();
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext ctx) {
		return SHAPE;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StargateDialerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.STARGATE_DIALER.get(), StargateDialerBlockEntity::tick);
	}
}
