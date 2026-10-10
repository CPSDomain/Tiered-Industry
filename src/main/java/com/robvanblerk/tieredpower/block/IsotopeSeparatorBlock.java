package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.IsotopeSeparatorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class IsotopeSeparatorBlock extends MachineBlock {
	public IsotopeSeparatorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new IsotopeSeparatorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ISOTOPE_SEPARATOR.get(), IsotopeSeparatorBlockEntity::tick);
	}
}
