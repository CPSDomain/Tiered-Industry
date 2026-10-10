package com.robvanblerk.tieredpower.spatial;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Spatial Cell: holds one captured cube of blocks (3, 7 or 15 wide). The blocks themselves are kept with the world. */
public class SpatialCellItem extends Item {
	private final int size;

	public SpatialCellItem(int size, Properties properties) {
		super(properties);
		this.size = size;
	}

	public int size() {
		return size;
	}

	public static boolean isFull(ItemStack stack) {
		return stack.hasTag() && stack.getTag().hasUUID("space");
	}

	public static @Nullable UUID space(ItemStack stack) {
		return isFull(stack) ? stack.getTag().getUUID("space") : null;
	}

	public static void fill(ItemStack stack, UUID id, int blocks) {
		var tag = stack.getOrCreateTag();
		tag.putUUID("space", id);
		tag.putInt("blocks", blocks);
	}

	public static void empty(ItemStack stack) {
		if (!stack.hasTag()) return;
		stack.getTag().remove("space");
		stack.getTag().remove("blocks");
		if (stack.getTag().isEmpty()) stack.setTag(null);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(size + " x " + size + " x " + size + " blocks").withStyle(ChatFormatting.GRAY));
		if (isFull(stack)) tooltip.add(Component.literal("Holds a space with " + stack.getTag().getInt("blocks") + " blocks").withStyle(ChatFormatting.AQUA));
		else tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return isFull(stack);
	}
}
