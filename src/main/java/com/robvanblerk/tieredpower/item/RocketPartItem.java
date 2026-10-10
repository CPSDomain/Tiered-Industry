package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A rocket program part with a line of tooltip text. */
public class RocketPartItem extends Item {
	private final String hint;

	public RocketPartItem(String hint, Properties properties) {
		super(properties);
		this.hint = hint;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(hint).withStyle(ChatFormatting.GRAY));
	}
}
