package com.robvanblerk.tieredpower.conveyor;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Belt Splitter: items reaching the middle go out to the left, right and straight on in turn - only to sides that
 * have a belt or an inventory. Mobs and players just go straight on.
 */
public class SplitterConveyorBlock extends ConveyorBlock {
	public SplitterConveyorBlock(int tier, Properties properties) {
		super(tier, properties);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected @Nullable Direction route(BlockState state, Level level, BlockPos pos, Entity entity, double t) {
		Direction facing = state.getValue(FACING);
		if (!(entity instanceof ItemEntity) || t < 0.5) return facing;
		if (level.isClientSide()) return null; // the server decides and tells the client
		int known = decision(entity, pos);
		if (known >= 0) return Direction.from3DDataValue(known);
		List<Direction> outs = new ArrayList<>();
		for (Direction d : new Direction[] {facing.getCounterClockWise(), facing, facing.getClockWise()})
			if (canOutput(level, pos, d)) outs.add(d);
		Direction pick = outs.isEmpty() ? facing
				: level.getBlockEntity(pos) instanceof ConveyorBlockEntity be ? be.nextOutput(outs) : outs.get(0);
		decide(entity, pos, pick);
		return pick;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, level, tooltip, flag);
		tooltip.add(Component.literal("Shares items between left, right and straight on").withStyle(ChatFormatting.DARK_GRAY));
	}
}
