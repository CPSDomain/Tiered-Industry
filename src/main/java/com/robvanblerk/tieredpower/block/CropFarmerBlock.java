package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.CropFarmerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class CropFarmerBlock extends MachineBlock {
	public CropFarmerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CropFarmerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CROP_FARMER.get(), CropFarmerBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.CROP_BREAK;
	}

	@Override
	protected float soundChance() {
		return 0.06f;
	}

	@Override
	protected float soundVolume() {
		return 0.3f;
	}

	@Override
	protected float soundPitch() {
		return 1.0f;
	}
}
