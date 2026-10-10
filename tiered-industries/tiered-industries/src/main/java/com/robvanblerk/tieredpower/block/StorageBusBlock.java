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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.robvanblerk.tieredpower.block.entity.StorageBusBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.StorageNetworkBlock;

/** Import or Export Bus: place it against an inventory; its face points into that inventory. */
public class StorageBusBlock extends BaseEntityBlock implements StorageNetworkBlock {
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	private final boolean importing;

	// A plate on the facing side plus a short neck, in the model's north orientation.
	private static final VoxelShape NORTH = Block.box(1, 1, 0, 15, 15, 3); // a flat plate against the inventory

	public StorageBusBlock(Properties properties, boolean importing) {
		super(properties);
		this.importing = importing;
		registerDefaultState(PanelCables.none(stateDefinition.any().setValue(FACING, Direction.NORTH)));
	}

	public boolean isImport() {
		return importing;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
		PanelCables.addProperties(builder);
	}

	/** Faces the block you clicked on. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx) {
		BlockState state = defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite());
		return PanelCables.update(state, ctx.getLevel(), ctx.getClickedPos(), state.getValue(FACING));
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
			case NORTH -> NORTH;
			case SOUTH -> Block.box(1, 1, 13, 15, 15, 16);
			case WEST -> Block.box(0, 1, 1, 3, 15, 15);
			case EAST -> Block.box(13, 1, 1, 16, 15, 15);
			case DOWN -> Block.box(1, 0, 1, 15, 3, 15);
			case UP -> Block.box(1, 13, 1, 15, 16, 15);
		};
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide() && !com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)) return InteractionResult.CONSUME;
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof StorageBusBlockEntity be) player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StorageBusBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.STORAGE_BUS.get(), StorageBusBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return PanelCables.update(state, dir, neighbour, state.getValue(FACING));
	}
}
