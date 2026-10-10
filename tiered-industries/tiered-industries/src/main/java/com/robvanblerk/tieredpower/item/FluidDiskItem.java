package com.robvanblerk.tieredpower.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Holds fluids and gases for a storage network. Capacity is in buckets; contents stay on the disk. */
public class FluidDiskItem extends Item {
	public static final int MAX_TYPES = 63;
	private final long buckets;

	public FluidDiskItem(long buckets, Properties properties) {
		super(properties);
		this.buckets = buckets;
	}

	public long getCapacityMb() {
		return buckets * 1_000;
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return StorageDiskItem.used(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13f * Math.min(1f, (float) StorageDiskItem.used(stack) / getCapacityMb()));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x3F76E4;
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		String cap = buckets >= 1_000_000_000_000L ? "unlimited" : String.format("%,d", buckets);
		tooltip.add(Component.literal(String.format("%,d / %s buckets", StorageDiskItem.used(stack) / 1_000, cap)).withStyle(ChatFormatting.BLUE));
		tooltip.add(Component.literal(StorageDiskItem.types(stack) + " / " + MAX_TYPES + " fluid and gas types").withStyle(ChatFormatting.GRAY));
		int parts = com.robvanblerk.tieredpower.storage.DiskConfig.fluidPartition(stack).size();
		int prio = com.robvanblerk.tieredpower.storage.DiskConfig.priority(stack);
		if (parts > 0) tooltip.add(Component.literal("Partitioned: only " + parts + " chosen type" + (parts == 1 ? "" : "s")).withStyle(ChatFormatting.GOLD));
		if (prio != 0) tooltip.add(Component.literal("Priority " + (prio > 0 ? "+" : "") + prio).withStyle(ChatFormatting.GOLD));
		tooltip.add(Component.literal("Goes in a Drive Bay. Keeps its contents when removed.").withStyle(ChatFormatting.DARK_GRAY));
	}
}
