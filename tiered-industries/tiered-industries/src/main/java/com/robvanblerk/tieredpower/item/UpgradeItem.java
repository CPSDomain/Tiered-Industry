package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Speed or Efficiency upgrade card. Goes in a machine's matching upgrade slot (up to 8). */
public class UpgradeItem extends Item {
	public enum Kind { SPEED, EFFICIENCY }

	private final Kind kind;

	public UpgradeItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind getKind() {
		return kind;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		String key = kind == Kind.SPEED ? "tooltip.tieredpower.speed_upgrade" : "tooltip.tieredpower.efficiency_upgrade";
		tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip.tieredpower.upgrade_slots").withStyle(ChatFormatting.DARK_GRAY));
	}
}
