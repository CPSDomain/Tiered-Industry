package com.robvanblerk.tieredpower.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;

/** Goes inside the Energy Bank; each one adds its capacity to the bank. */
public class EnergyCellBlock extends Block {
	private final long capacity;

	public EnergyCellBlock(long capacity, Properties properties) {
		super(properties);
		this.capacity = capacity;
	}

	public long getCapacity() {
		return capacity;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(String.format("Adds %,d FE to an Energy Bank", capacity)).withStyle(ChatFormatting.GRAY));
	}
}
