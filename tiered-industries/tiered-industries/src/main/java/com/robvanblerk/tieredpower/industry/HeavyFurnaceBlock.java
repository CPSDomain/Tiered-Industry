package com.robvanblerk.tieredpower.industry;

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

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class HeavyFurnaceBlock extends MachineBlock {
	public HeavyFurnaceBlock(Properties properties) {
		super(properties);
	}

	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HeavyFurnaceBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.HEAVY_FURNACE.get(), HeavyFurnaceBlockEntity::tick);
	}

	/** An empty bucket takes Creosote Oil out of the Coke Oven. */
	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
			return InteractionResult.sidedSuccess(level.isClientSide());
		return super.use(state, level, pos, player, hand, hit);
	}
}
