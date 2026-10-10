package com.robvanblerk.tieredpower.storage;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * A disk's partition and priority, kept on the disk itself (set in a Disk Workbench). Item disks keep a list of items,
 * fluid disks a list of fluids. An empty partition accepts anything. Higher priority disks are filled first.
 */
public final class DiskConfig {
	public static final int MAX_ITEMS = 18, MIN_PRIORITY = -9, MAX_PRIORITY = 9;

	public static int priority(ItemStack disk) {
		CompoundTag tag = disk.getTag();
		return tag == null ? 0 : tag.getInt("Priority");
	}

	public static void setPriority(ItemStack disk, int priority) {
		disk.getOrCreateTag().putInt("Priority", Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority)));
	}

	public static List<ItemStack> itemPartition(ItemStack disk) {
		List<ItemStack> out = new ArrayList<>();
		CompoundTag tag = disk.getTag();
		if (tag == null) return out;
		ListTag list = tag.getList("Partition", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			ItemStack s = ItemStack.of(list.getCompound(i));
			if (!s.isEmpty()) out.add(s);
		}
		return out;
	}

	public static void setItemPartition(ItemStack disk, List<ItemStack> items) {
		ListTag list = new ListTag();
		for (ItemStack s : items) if (!s.isEmpty() && list.size() < MAX_ITEMS) list.add(s.copyWithCount(1).save(new CompoundTag()));
		if (list.isEmpty()) disk.getOrCreateTag().remove("Partition"); else disk.getOrCreateTag().put("Partition", list);
	}

	public static List<FluidStack> fluidPartition(ItemStack disk) {
		List<FluidStack> out = new ArrayList<>();
		CompoundTag tag = disk.getTag();
		if (tag == null) return out;
		ListTag list = tag.getList("FluidPartition", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			FluidStack f = FluidStack.loadFluidStackFromNBT(list.getCompound(i));
			if (!f.isEmpty()) out.add(f);
		}
		return out;
	}

	public static void setFluidPartition(ItemStack disk, List<FluidStack> fluids) {
		ListTag list = new ListTag();
		for (FluidStack f : fluids) if (!f.isEmpty() && list.size() < MAX_ITEMS) list.add(new FluidStack(f, 1).writeToNBT(new CompoundTag()));
		if (list.isEmpty()) disk.getOrCreateTag().remove("FluidPartition"); else disk.getOrCreateTag().put("FluidPartition", list);
	}

	private DiskConfig() {}
}
