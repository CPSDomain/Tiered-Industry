package com.robvanblerk.tieredpower.logic;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;
import com.robvanblerk.tieredpower.storage.StorageSecurity;

/** Logic Controller: joins the storage network and gives out redstone signals according to its rules. */
public class LogicControllerBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public LogicControllerBlock(Properties properties) {
		super(properties);
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LogicControllerBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.LOGIC_CONTROLLER.get(), LogicControllerBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && StorageSecurity.check(player, level, pos) && level.getBlockEntity(pos) instanceof LogicControllerBlockEntity be) player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override @SuppressWarnings("deprecation") public boolean isSignalSource(BlockState state) { return true; }

	/** 'dir' points from the block asking towards the controller, so the controller's face is the opposite. */
	@Override
	@SuppressWarnings("deprecation")
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return level.getBlockEntity(pos) instanceof LogicControllerBlockEntity be ? be.signalOut(dir.getOpposite()) : 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		boolean gone = !state.is(newState.getBlock());
		super.onRemove(state, level, pos, newState, moving);
		if (gone) level.updateNeighborsAt(pos, this); // signals switch off
	}
}
