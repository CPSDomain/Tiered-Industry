package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.StorageMonitorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Shows the live count of one item in the storage network. Right-click with an item to choose it; sneak + right-click with an empty hand to clear. */
public class StorageMonitorBlock extends BaseEntityBlock implements com.robvanblerk.tieredpower.storage.StorageNetworkBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public StorageMonitorBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof StorageMonitorBlockEntity be ? be.comparatorSignal() : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageMonitorBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.STORAGE_MONITOR.get(), StorageMonitorBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof StorageMonitorBlockEntity be)) return net.minecraft.world.InteractionResult.PASS;
		if (!com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return net.minecraft.world.InteractionResult.CONSUME;
		var held = player.getItemInHand(hand);
		if (!held.isEmpty()) {
			be.setShown(held);
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Showing " + held.getHoverName().getString()), true);
		} else if (player.isShiftKeyDown()) {
			be.setShown(net.minecraft.world.item.ItemStack.EMPTY);
			player.displayClientMessage(net.minecraft.network.chat.Component.literal("Monitor cleared"), true);
		} else if (!be.getShown().isEmpty()) {
			long n = be.getCount();
			player.displayClientMessage(net.minecraft.network.chat.Component.literal(be.getShown().getHoverName().getString() + ": "
					+ (n < 0 ? "no powered storage network" : String.format("%,d", n))), true);
		}
		return net.minecraft.world.InteractionResult.CONSUME;
	}
}
