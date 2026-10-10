package com.robvanblerk.tieredpower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/**
 * A flat Storage (or Crafting) Terminal that sits on a wall, floor or ceiling, like AE2's terminals.
 * Its screen faces away from the block it's placed on; put a Storage Cable (or any storage block) behind it.
 */
public class TerminalPanelBlock extends DirectionalBlock implements StorageNetworkBlock {
	private final boolean crafting;

	public TerminalPanelBlock(Properties properties, boolean crafting) {
		super(properties);
		this.crafting = crafting;
		registerDefaultState(PanelCables.none(stateDefinition.any().setValue(FACING, Direction.NORTH)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		PanelCables.addProperties(builder);
	}

	/** The screen faces out from the surface you clicked. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		return PanelCables.update(defaultBlockState().setValue(FACING, ctx.getClickedFace()), ctx.getLevel(), ctx.getClickedPos());
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
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return switch (state.getValue(FACING)) {
			case NORTH -> Block.box(1, 1, 13, 15, 15, 16);
			case SOUTH -> Block.box(1, 1, 0, 15, 15, 3);
			case WEST -> Block.box(13, 1, 1, 16, 15, 15);
			case EAST -> Block.box(0, 1, 1, 3, 15, 15);
			case UP -> Block.box(1, 0, 1, 15, 3, 15);
			case DOWN -> Block.box(1, 13, 1, 15, 16, 15);
		};
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide()) StorageTerminalBlock.open(player, pos, crafting);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return PanelCables.update(state, dir, neighbour);
	}
}
