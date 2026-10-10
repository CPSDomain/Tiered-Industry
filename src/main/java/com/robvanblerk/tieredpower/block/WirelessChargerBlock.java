package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.WirelessChargerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class WirelessChargerBlock extends MachineBlock {
	public WirelessChargerBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WirelessChargerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.WIRELESS_CHARGER.get(), WirelessChargerBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.BEACON_AMBIENT;
	}

	@Override
	protected float soundChance() {
		return 0.03f;
	}

	@Override
	protected float soundVolume() {
		return 0.2f;
	}

	@Override
	protected float soundPitch() {
		return 1.8f;
	}
}
