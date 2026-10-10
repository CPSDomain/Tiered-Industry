package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.block.entity.LaunchControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Launch Controller (a desk console) and Receiver Dish (a dish on a post): both face the player who placed them. */
public class LaunchControllerBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final VoxelShape DESK = Shapes.or(Block.box(1, 0, 1, 15, 10, 15), Block.box(1, 10, 1, 15, 14, 8));
	private static final VoxelShape DISH = Shapes.or(Block.box(3, 0, 3, 13, 3, 13), Block.box(6, 3, 6, 10, 9, 10), Block.box(0, 8, 0, 16, 16, 16));
	private final boolean dish;

	public LaunchControllerBlock(boolean dish, Properties properties) {
		super(properties);
		this.dish = dish;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
	@Override @SuppressWarnings("deprecation") public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override @SuppressWarnings("deprecation") public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return dish ? DISH : DESK; }

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return dish ? new ReceiverDishBlockEntity(pos, state) : new LaunchControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) return null;
		return dish ? createTickerHelper(type, ModBlockEntities.RECEIVER_DISH.get(), ReceiverDishBlockEntity::tick)
				: createTickerHelper(type, ModBlockEntities.LAUNCH_CONTROLLER.get(), LaunchControllerBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		var be = level.getBlockEntity(pos);
		if (be instanceof LaunchControllerBlockEntity c) {
			if (net.minecraftforge.fluids.FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
					&& net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
				return InteractionResult.CONSUME; // a bucket of Rocket Fuel
			player.openMenu(c);
		} else if (be instanceof ReceiverDishBlockEntity d) {
			if (player.isShiftKeyDown()) {
				d.release();
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("Dish released its satellite"), true);
			} else if (d.getSatelliteId() >= 0) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal(String.format("Linked to Solar Satellite #%d - %,d FE/t (sneak + right-click to release)",
						d.getSatelliteId(), d.getGenerating())).withStyle(net.minecraft.ChatFormatting.AQUA), true);
			} else {
				var sat = d.claim(player);
				player.displayClientMessage(sat == null
						? net.minecraft.network.chat.Component.literal("None of your satellites are free - launch one from a Launch Pad").withStyle(net.minecraft.ChatFormatting.RED)
						: net.minecraft.network.chat.Component.literal("Now receiving power from Solar Satellite #" + sat.id).withStyle(net.minecraft.ChatFormatting.GREEN), true);
			}
		}
		return InteractionResult.CONSUME;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock())) {
			var be = level.getBlockEntity(pos);
			if (be instanceof LaunchControllerBlockEntity c) {
				c.removed();
				net.minecraft.world.Containers.dropContents(level, pos, c.items);
			} else if (be instanceof ReceiverDishBlockEntity d) d.release();
		}
		super.onRemove(state, level, pos, newState, moving);
	}
}
