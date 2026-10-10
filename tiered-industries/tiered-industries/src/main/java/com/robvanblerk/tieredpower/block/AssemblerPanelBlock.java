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
 * Assembler Panel: a flat Molecular Assembler - the same 9 pattern slots, screen and upgrades - stuck onto a machine's
 * face (sneak + right-click to place it on a machine). Connect it to the storage network with Storage Cable.
 */
public class AssemblerPanelBlock extends DirectionalBlock implements StorageNetworkBlock, net.minecraft.world.level.block.EntityBlock {
	public AssemblerPanelBlock(Properties properties) {
		super(properties);
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
	public @org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends net.minecraft.world.level.block.entity.BlockEntity> @org.jetbrains.annotations.Nullable net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
			net.minecraft.world.level.Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		if (level.isClientSide() || type != com.robvanblerk.tieredpower.registry.ModBlockEntities.MOLECULAR_ASSEMBLER.get()) return null;
		return (lvl, pos, st, be) -> com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity.tick(lvl, pos, st,
				(com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity) be);
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (!level.isClientSide() && com.robvanblerk.tieredpower.storage.StorageSecurity.check(player, level, pos)
				&& level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity be)
			player.openMenu(be);
		return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
	}

	/** Patterns and upgrades drop when it's broken. */
	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity be)
			net.minecraft.world.Containers.dropContents(level, pos, be);
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return PanelCables.update(state, dir, neighbour);
	}
}
