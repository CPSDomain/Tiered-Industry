package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.StorageNetwork;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** The heart of an item storage network. Right-click for its status. */
public class StorageControllerBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public StorageControllerBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LIT);
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
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && !com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return InteractionResult.CONSUME;
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageControllerBlockEntity c) {
			player.sendSystemMessage(Component.literal("-- Storage Controller --").withStyle(ChatFormatting.GOLD));
			switch (c.getProblem()) {
				case NO_POWER -> player.sendSystemMessage(Component.literal("Offline: needs " + c.costPerTick() + " FE/t - connect it to power").withStyle(ChatFormatting.RED));
				case TWO_CONTROLLERS -> player.sendSystemMessage(Component.literal("Offline: this network has more than one controller - remove the extras").withStyle(ChatFormatting.RED));
				default -> {
					StorageNetwork.Stats s = c.stats();
					player.sendSystemMessage(Component.literal("Online, using " + c.costPerTick() + " FE/t").withStyle(ChatFormatting.GREEN));
					player.sendSystemMessage(Component.literal(String.format("%,d / %,d items, %d item types", s.used(), s.capacity(), s.types())).withStyle(ChatFormatting.AQUA));
					if (s.fluidDisks() > 0)
						player.sendSystemMessage(Component.literal(String.format("%,d / %,d buckets of fluid and gas, %d types", s.fluidUsed() / 1000, s.fluidCapacity() / 1000,
								s.fluidTypes())).withStyle(ChatFormatting.BLUE));
					player.sendSystemMessage(Component.literal(s.disks() + " item disks and " + s.fluidDisks() + " fluid disks in " + s.bays() + " Drive Bays")
							.withStyle(ChatFormatting.GRAY));
					if (s.disks() + s.fluidDisks() == 0) player.sendSystemMessage(Component.literal("Add a Drive Bay with Storage Disks to store items").withStyle(ChatFormatting.YELLOW));
				}
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.STORAGE_CONTROLLER.get(), StorageControllerBlockEntity::tick);
	}
}
