package com.robvanblerk.tieredpower.stargate;

import java.util.List;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Stargate Frame. Build a standing 7x7 square of them, then right-click one with an empty hand: the square turns into
 * a round Stargate (the blocks hide and the bottom-middle one draws the ring). Breaking any piece turns it back into
 * blocks. A plain square ring works too - forming it is only for the looks.
 */
public class StargateFrameBlock extends Block implements EntityBlock {
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	/** The bottom-middle block of a formed gate: it holds the ring's block entity and draws it. */
	public static final BooleanProperty MASTER = BooleanProperty.create("master");
	/** Formed blocks the round ring doesn't pass through (the corners) can be walked through. */
	public static final BooleanProperty RING = BooleanProperty.create("ring");
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

	public StargateFrameBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FORMED, false).setValue(MASTER, false).setValue(RING, true).setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FORMED, MASTER, RING, AXIS);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
		return state.getValue(FORMED) && !state.getValue(RING) ? Shapes.empty() : Shapes.block();
	}

	@Override
	@SuppressWarnings("deprecation")
	public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(FORMED) ? 1.0f : super.getShadeBrightness(state, level, pos);
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(FORMED);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		if (state.getValue(FORMED)) {
			player.displayClientMessage(Component.literal("Stargate ready - dial it with a Stargate Dialler within 8 blocks").withStyle(ChatFormatting.AQUA), true);
			return InteractionResult.CONSUME;
		}
		StargateRing.Shape shape = StargateRing.find(level, pos);
		if (shape == null) {
			player.displayClientMessage(Component.literal("Not a complete ring: build a standing 7x7 square of Stargate Frame, empty inside").withStyle(ChatFormatting.RED), false);
			return InteractionResult.CONSUME;
		}
		StargateRing.form(level, shape, true);
		level.playSound(null, shape.master(), SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.7f);
		player.displayClientMessage(Component.literal("Stargate formed").withStyle(ChatFormatting.GREEN), true);
		return InteractionResult.CONSUME;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!state.is(newState.getBlock()) && state.getValue(FORMED) && !level.isClientSide()) StargateRing.unformAround(level, pos);
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(MASTER) ? new StargateRingBlockEntity(pos, state) : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (!state.getValue(MASTER) || type != com.robvanblerk.tieredpower.registry.ModBlockEntities.STARGATE_RING.get()) return null;
		return (lvl, pos, st, be) -> StargateRingBlockEntity.tick(lvl, pos, st, (StargateRingBlockEntity) be);
	}

	@Override
	public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
		tooltip.add(Component.literal("Build a 7x7 square, then right-click it").withStyle(ChatFormatting.GRAY));
	}
}
