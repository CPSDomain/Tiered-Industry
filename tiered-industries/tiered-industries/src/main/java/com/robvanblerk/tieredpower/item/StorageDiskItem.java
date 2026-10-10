package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Holds items for a storage network. Contents are kept on the disk itself, so they survive being moved. */
public class StorageDiskItem extends Item {
	public static final int MAX_TYPES = 63;
	private final long capacity;

	public StorageDiskItem(long capacity, Properties properties) {
		super(properties);
		this.capacity = capacity;
	}

	public long getCapacity() {
		return capacity;
	}

	public static long used(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getLong("Used");
	}

	public static int types(ItemStack stack) {
		CompoundTag tag = stack.getTag();
		return tag == null ? 0 : tag.getInt("Types");
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return used(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * Math.min(1f, (float) used(stack) / capacity));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x55AAFF;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		String cap = capacity >= 1_000_000_000_000_000L ? "unlimited" : String.format("%,d", capacity);
		tooltip.add(Component.literal(String.format("%,d / %s items", used(stack), cap)).withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.literal(types(stack) + " / " + MAX_TYPES + " item types").withStyle(ChatFormatting.GRAY));
		int parts = com.robvanblerk.tieredpower.storage.DiskConfig.itemPartition(stack).size();
		int prio = com.robvanblerk.tieredpower.storage.DiskConfig.priority(stack);
		if (parts > 0) tooltip.add(Component.literal("Partitioned: only " + parts + " chosen type" + (parts == 1 ? "" : "s")).withStyle(ChatFormatting.GOLD));
		if (prio != 0) tooltip.add(Component.literal("Priority " + (prio > 0 ? "+" : "") + prio).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal("Goes in a Drive Bay. Keeps its items when removed.").withStyle(ChatFormatting.DARK_GRAY));
	}
}
