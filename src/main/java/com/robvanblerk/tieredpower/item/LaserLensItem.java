package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Put in a Laser Drill to make its ore far more likely (x10). Several lenses stack. */
public class LaserLensItem extends Item {
	private final String ore;
	private final TagKey<Item> tag;

	public LaserLensItem(String ore, Properties properties) {
		super(properties);
		this.ore = ore;
		this.tag = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores/" + ore));
	}

	public TagKey<Item> oreTag() {
		return tag;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("In a Laser Drill: " + ore + " ores x10 as likely").withStyle(ChatFormatting.AQUA));
	}
}
