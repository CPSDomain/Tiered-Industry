package com.robvanblerk.tieredpower.conveyor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Filter Belt: items in its filter turn off to one side (left unless switched), everything else goes straight on.
 * Right-click with an item to add it to the filter (again to remove it); empty hand to list the filter; sneak +
 * right-click with an empty hand to switch sides.
 */
public class FilterConveyorBlock extends ConveyorBlock {
	/** Which side matching items leave by - only for the model's arrow. */
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");

	public FilterConveyorBlock(int tier, Properties properties) {
		super(tier, properties);
		registerDefaultState(defaultBlockState().setValue(RIGHT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, RIGHT);
	}

	@Override
	protected @Nullable Direction route(BlockState state, Level level, BlockPos pos, Entity entity, double t) {
		Direction facing = state.getValue(FACING);
		if (!(entity instanceof ItemEntity item) || t < 0.5) return facing;
		if (level.isClientSide()) return null;
		int known = decision(entity, pos);
		if (known >= 0) return Direction.from3DDataValue(known);
		Direction side = state.getValue(RIGHT) ? facing.getClockWise() : facing.getCounterClockWise();
		Direction pick = level.getBlockEntity(pos) instanceof ConveyorBlockEntity be && be.matches(item.getItem()) && canOutput(level, pos, side) ? side : facing;
		decide(entity, pos, pick);
		return pick;
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof ConveyorBlockEntity be)) return InteractionResult.PASS;
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		ItemStack held = player.getItemInHand(hand);
		if (!held.isEmpty()) {
			boolean had = be.matches(held);
			boolean added = be.toggle(held);
			String name = held.getHoverName().getString();
			player.displayClientMessage(Component.literal(added ? "Added " + name + " to the filter"
					: had ? "Removed " + name + " from the filter" : "Filter is full (" + ConveyorBlockEntity.FILTER_SIZE + " items)")
					.withStyle(added ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
		} else if (player.isShiftKeyDown()) {
			be.flipSide();
			level.setBlock(pos, state.setValue(RIGHT, be.sortRight()), 3);
			player.displayClientMessage(Component.literal("Filtered items now go " + (be.sortRight() ? "right" : "left")).withStyle(ChatFormatting.YELLOW), true);
		} else {
			if (be.filter().isEmpty()) {
				player.displayClientMessage(Component.literal("Filter empty - right-click with an item to add it").withStyle(ChatFormatting.GRAY), false);
			} else {
				StringBuilder s = new StringBuilder("Sends " + (be.sortRight() ? "right" : "left") + ": ");
				for (int i = 0; i < be.filter().size(); i++) s.append(i > 0 ? ", " : "").append(be.filter().get(i).getHoverName().getString());
				player.displayClientMessage(Component.literal(s.toString()).withStyle(ChatFormatting.AQUA), false);
			}
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("Right-click with an item to filter it to the side").withStyle(ChatFormatting.DARK_GRAY));
	}
}
