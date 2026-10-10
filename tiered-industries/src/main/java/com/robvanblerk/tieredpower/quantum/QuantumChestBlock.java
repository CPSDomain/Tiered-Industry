package com.robvanblerk.tieredpower.quantum;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Quantum Chest block: right-click to open (unless holding a Quantum Linker). */
public class QuantumChestBlock extends BaseEntityBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public QuantumChestBlock(Properties properties) {
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

	@Override @SuppressWarnings("deprecation") public BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }
	@Override @SuppressWarnings("deprecation") public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new QuantumChestBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.QUANTUM_CHEST.get(), QuantumChestBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player.getItemInHand(hand).is(ModBlocks.QUANTUM_LINKER.get())) return InteractionResult.PASS; // the linker handles it
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof QuantumChestBlockEntity be) player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof QuantumChestBlockEntity be) {
			Containers.dropContents(level, pos, be);
			if (be.isMain() && level instanceof ServerLevel server) {
				// anything still in transit to this chest drops here too
				for (var s : QuantumNetworks.get(server.getServer()).removeMain(be.getId()))
					Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
			}
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override @SuppressWarnings("deprecation") public boolean hasAnalogOutputSignal(BlockState state) { return true; }

	@Override
	@SuppressWarnings("deprecation")
	public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof QuantumChestBlockEntity be ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be) : 0;
	}
}
