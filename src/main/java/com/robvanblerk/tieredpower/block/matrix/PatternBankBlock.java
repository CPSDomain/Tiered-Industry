package com.robvanblerk.tieredpower.block.matrix;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.PatternBankBlockEntity;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Holds 27 patterns inside an Assembly Matrix (filled through the Matrix Controller). Drops its patterns when broken. */
public class PatternBankBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public PatternBankBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PatternBankBlockEntity be) Containers.dropContents(level, pos, be.container());
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PatternBankBlockEntity(pos, state);
	}
}
