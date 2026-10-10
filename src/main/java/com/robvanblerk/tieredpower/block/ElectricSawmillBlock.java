package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.block.entity.ElectricSawmillBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ProcessingMachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class ElectricSawmillBlock extends MachineBlock {
	public ElectricSawmillBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (false && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new ElectricSawmillBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ELECTRIC_SAWMILL.get(), ProcessingMachineBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.AXE_STRIP;
	}

	@Override
	protected float soundChance() {
		return 0.08f;
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
