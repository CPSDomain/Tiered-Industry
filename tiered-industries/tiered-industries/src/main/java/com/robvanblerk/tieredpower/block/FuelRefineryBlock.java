package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.FuelRefineryBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

public class FuelRefineryBlock extends MachineBlock {
	public FuelRefineryBlock(Properties properties) {
		super(properties);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FuelRefineryBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FUEL_REFINERY.get(), FuelRefineryBlockEntity::tick);
	}
	/** Right-click with a bucket or tank to pour fluid in (handy for testing). */
	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (net.minecraftforge.fluids.FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
				&& net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
			return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
		return super.use(state, level, pos, player, hand, hit);
	}
}
