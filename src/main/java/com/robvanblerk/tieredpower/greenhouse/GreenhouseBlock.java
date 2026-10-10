package com.robvanblerk.tieredpower.greenhouse;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** Sprinkler and Grow Lamp: hang them above the crops (on a ceiling, or on top of a post). */
public class GreenhouseBlock extends BaseEntityBlock {
	private static final VoxelShape SPRINKLER = Shapes.or(Block.box(7, 10, 7, 9, 16, 9), Block.box(4, 8, 4, 12, 10, 12));
	private static final VoxelShape LAMP = Shapes.or(Block.box(7.5, 10, 7.5, 8.5, 16, 8.5), Block.box(3, 5, 3, 13, 10, 13));
	private final boolean sprinkler;

	public GreenhouseBlock(boolean sprinkler, Properties properties) {
		super(properties);
		this.sprinkler = sprinkler;
		if (!sprinkler) registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(BlockStateProperties.LIT); // only the lamp lights up; the sprinkler ignores it
	}

	@Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override @SuppressWarnings("deprecation") public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return sprinkler ? SPRINKLER : LAMP; }
	@Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GreenhouseBlockEntity(pos, state); }

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.GREENHOUSE.get(), GreenhouseBlockEntity::tick);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (sprinkler && FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent() && FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
			return InteractionResult.sidedSuccess(level.isClientSide());
		return InteractionResult.PASS;
	}
}
