package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.DiskWorkbenchBlockEntity;

/** Partition and prioritise storage disks. */
public class DiskWorkbenchBlock extends BaseEntityBlock {
	public DiskWorkbenchBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof DiskWorkbenchBlockEntity be) player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DiskWorkbenchBlockEntity be) Containers.dropContents(level, pos, be.disk());
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DiskWorkbenchBlockEntity(pos, state);
	}
}
