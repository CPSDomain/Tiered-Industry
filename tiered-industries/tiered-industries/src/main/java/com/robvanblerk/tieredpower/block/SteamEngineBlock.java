package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class SteamEngineBlock extends MachineBlock {
	public SteamEngineBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new SteamEngineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.STEAM_ENGINE.get(), SteamEngineBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.PISTON_EXTEND;
	}

	@Override
	protected float soundChance() {
		return 0.08f;
	}

	@Override
	protected float soundVolume() {
		return 0.25f;
	}

	@Override
	protected float soundPitch() {
		return 0.7f;
	}
}
