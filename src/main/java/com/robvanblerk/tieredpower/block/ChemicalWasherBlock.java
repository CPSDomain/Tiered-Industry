package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.ChemicalWasherBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class ChemicalWasherBlock extends MachineBlock {
	public ChemicalWasherBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ChemicalWasherBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CHEMICAL_WASHER.get(), ChemicalWasherBlockEntity::tick);
	}
}
