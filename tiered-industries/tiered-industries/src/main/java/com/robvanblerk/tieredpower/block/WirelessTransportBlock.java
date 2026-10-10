package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity;
import com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Wireless Sender (receiver = false) and Wireless Receiver (receiver = true) for items and fluids. */
public class WirelessTransportBlock extends BaseEntityBlock {
	private final boolean receiver;

	public WirelessTransportBlock(boolean receiver, Properties properties) {
		super(properties);
		this.receiver = receiver;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return receiver ? new WirelessReceiverBlockEntity(pos, state) : new WirelessSenderBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!receiver || level.isClientSide()) return null;
		return createTickerHelper(type, ModBlockEntities.WIRELESS_RECEIVER.get(), WirelessReceiverBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock())) {
			var be = level.getBlockEntity(pos);
			var items = be instanceof WirelessSenderBlockEntity s ? s.items : be instanceof WirelessReceiverBlockEntity r ? r.items : null;
			if (items != null) for (int i = 0; i < items.getSlots(); i++)
				net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, items.getStackInSlot(i));
		}
		super.onRemove(state, level, pos, newState, moving);
	}
}
