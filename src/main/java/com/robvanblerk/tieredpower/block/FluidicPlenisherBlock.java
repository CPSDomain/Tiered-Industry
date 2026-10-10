package com.robvanblerk.tieredpower.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import com.robvanblerk.tieredpower.block.entity.FluidicPlenisherBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Fluidic Plenisher: pipe a fluid in and it fills the space below and around it. Right-click with a bucket to pour in. */
public class FluidicPlenisherBlock extends BaseEntityBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public FluidicPlenisherBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidicPlenisherBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FLUIDIC_PLENISHER.get(), FluidicPlenisherBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
		if (net.minecraftforge.fluids.FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
				&& net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
			return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
		return super.use(state, level, pos, player, hand, hit);
	}
}
