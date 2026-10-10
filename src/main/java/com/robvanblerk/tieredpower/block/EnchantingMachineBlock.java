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

import com.robvanblerk.tieredpower.block.entity.EnchantingMachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class EnchantingMachineBlock extends MachineBlock {
	public EnchantingMachineBlock(Properties properties) {
		super(properties);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
			return InteractionResult.sidedSuccess(level.isClientSide());
		}
		return super.use(state, level, pos, player, hand, hit);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnchantingMachineBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ENCHANTING_MACHINE.get(), EnchantingMachineBlockEntity::tick);
	}

	@Override
	protected net.minecraft.sounds.SoundEvent runningSound() {
		return net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE;
	}

	@Override
	protected float soundChance() {
		return 0.04f;
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
