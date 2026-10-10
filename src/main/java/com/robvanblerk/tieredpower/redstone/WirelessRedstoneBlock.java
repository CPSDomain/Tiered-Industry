package com.robvanblerk.tieredpower.redstone;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Wireless Redstone Transmitter (receiver = false) and Receiver (receiver = true). A transmitter sends the redstone
 * signal going into it on its channel; every receiver on the same channel gives out the strongest signal sent on it -
 * any distance, any dimension. Right-click to pick the channel (sneak to go back).
 */
public class WirelessRedstoneBlock extends BaseEntityBlock {
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	public final boolean receiver;

	public WirelessRedstoneBlock(boolean receiver, Properties properties) {
		super(properties);
		this.receiver = receiver;
		registerDefaultState(stateDefinition.any().setValue(POWER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(POWER);
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WirelessRedstoneBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.WIRELESS_REDSTONE.get(), WirelessRedstoneBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(pos) instanceof WirelessRedstoneBlockEntity be) {
			be.setChannel(be.getChannel() + (player.isShiftKeyDown() ? -1 : 1));
			level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.5f, 0.6f + be.getChannel() / 100f);
			player.displayClientMessage(Component.literal((receiver ? "Receiver" : "Transmitter") + " channel " + be.getChannel()
					+ " (right-click: next, sneak + right-click: back)").withStyle(ChatFormatting.RED), true);
		}
		return InteractionResult.CONSUME;
	}

	@Override @SuppressWarnings("deprecation") public boolean isSignalSource(BlockState state) { return receiver; }

	@Override
	@SuppressWarnings("deprecation")
	public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return receiver ? state.getValue(POWER) : 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
		return getSignal(state, level, pos, dir);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		boolean gone = !state.is(newState.getBlock());
		super.onRemove(state, level, pos, newState, moving);
		if (gone && receiver) level.updateNeighborsAt(pos, this);
	}
}
