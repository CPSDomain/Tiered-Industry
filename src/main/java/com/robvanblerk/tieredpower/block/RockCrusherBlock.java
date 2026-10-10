package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.ProcessingMachineBlockEntity;
import com.robvanblerk.tieredpower.block.entity.RockCrusherBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class RockCrusherBlock extends MachineBlock {
	public RockCrusherBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RockCrusherBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ROCK_CRUSHER.get(), ProcessingMachineBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.STONE_HIT;
	}

	@Override
	protected float soundChance() {
		return 0.12f;
	}

	@Override
	protected float soundVolume() {
		return 0.3f;
	}

	@Override
	protected float soundPitch() {
		return 0.8f;
	}
}
