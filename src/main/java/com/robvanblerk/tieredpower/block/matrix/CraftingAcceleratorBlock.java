package com.robvanblerk.tieredpower.block.matrix;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import com.robvanblerk.tieredpower.storage.CraftingTier;

/** Crafting Accelerator: an Assembly Matrix part that adds 2, 4, 8 or 16 crafts per cycle depending on its tier. */
public class CraftingAcceleratorBlock extends MatrixPartBlock {
	public CraftingAcceleratorBlock(Properties properties) {
		super(Kind.ACCELERATOR, properties);
		registerDefaultState(stateDefinition.any().setValue(CraftingTier.TIER, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CraftingTier.TIER);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		int t = CraftingTier.of(stack);
		tooltip.add(Component.literal(CraftingTier.NAMES[t] + ": +" + CraftingTier.ACCELERATOR_CRAFTS[t] + " crafts per cycle")
				.withStyle(Style.EMPTY.withColor(CraftingTier.COLOUR[t])));
	}
}
