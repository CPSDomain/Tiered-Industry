package com.robvanblerk.tieredpower.block;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Links the parts of an item storage network together. Carries no items or power itself. */
public class StorageCableBlock extends Block implements StorageNetworkBlock {
	private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);
	private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

	static {
		ARMS.put(Direction.NORTH, Block.box(6, 6, 0, 10, 10, 6));
		ARMS.put(Direction.SOUTH, Block.box(6, 6, 10, 10, 10, 16));
		ARMS.put(Direction.WEST, Block.box(0, 6, 6, 6, 10, 10));
		ARMS.put(Direction.EAST, Block.box(10, 6, 6, 16, 10, 10));
		ARMS.put(Direction.DOWN, Block.box(6, 0, 6, 10, 6, 10));
		ARMS.put(Direction.UP, Block.box(6, 10, 6, 10, 16, 10));
	}

	private final Map<BlockState, VoxelShape> shapeCache = new ConcurrentHashMap<>();

	public StorageCableBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any();
		for (BooleanProperty p : CableBlock.CONNECTIONS.values()) state = state.setValue(p, false);
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CableBlock.CONNECTIONS.values().toArray(new BooleanProperty[0]));
	}

	private static boolean connectsTo(LevelAccessor level, BlockPos pos, Direction dir) {
		return level.getBlockState(pos.relative(dir)).getBlock() instanceof StorageNetworkBlock;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState();
		for (Direction d : Direction.values()) state = state.setValue(CableBlock.CONNECTIONS.get(d), connectsTo(ctx.getLevel(), ctx.getClickedPos(), d));
		return state;
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return state.setValue(CableBlock.CONNECTIONS.get(dir), neighbourState.getBlock() instanceof StorageNetworkBlock);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapeCache.computeIfAbsent(state, s -> {
			VoxelShape shape = CORE;
			for (Direction d : Direction.values()) if (s.getValue(CableBlock.CONNECTIONS.get(d))) shape = Shapes.or(shape, ARMS.get(d));
			return shape;
		});
	}
}
