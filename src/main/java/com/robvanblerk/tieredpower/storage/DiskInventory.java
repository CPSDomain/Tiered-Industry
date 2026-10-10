package com.robvanblerk.tieredpower.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.item.StorageDiskItem;

/**
 * The contents of one Storage Disk. The disk's NBT is the real copy (so contents travel with the disk);
 * this keeps a parsed view and writes every change straight back to the stack.
 */
public class DiskInventory {
	private final ItemStack disk;
	private final StorageDiskItem type;
	private final Map<ItemKey, Long> items = new LinkedHashMap<>();
	private final Runnable onChanged;
	private long used;

	public DiskInventory(ItemStack disk, Runnable onChanged) {
		this.disk = disk;
		this.onChanged = onChanged;
		this.type = (StorageDiskItem) disk.getItem();
		CompoundTag tag = disk.getTag();
		if (tag != null && tag.contains("Stored", Tag.TAG_LIST)) {
			ListTag list = tag.getList("Stored", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag e = list.getCompound(i);
				ItemStack proto = ItemStack.of(e.getCompound("Item"));
				long count = e.getLong("Count");
				if (proto.isEmpty() || count <= 0) continue;
				items.merge(new ItemKey(proto), count, Long::sum);
				used += count;
			}
		}
	}

	public ItemStack disk() { return disk; }

	private java.util.Set<ItemKey> partition;

	/** The items this disk is limited to (empty = anything). Read from the disk when first needed. */
	public java.util.Set<ItemKey> partition() {
		if (partition == null) {
			partition = new java.util.HashSet<>();
			for (ItemStack s : DiskConfig.itemPartition(disk)) partition.add(new ItemKey(s));
		}
		return partition;
	}

	public boolean isPartitioned() { return !partition().isEmpty(); }
	public boolean accepts(ItemKey key) { return partition().isEmpty() || partition().contains(key); }
	public int priority() { return DiskConfig.priority(disk); }
	public Map<ItemKey, Long> items() { return items; }
	public long used() { return used; }
	public long capacity() { return type.getCapacity(); }
	public int types() { return items.size(); }

	/** How many of this item would fit. */
	public long space(ItemKey key) {
		if (!accepts(key)) return 0;
		if (!items.containsKey(key) && items.size() >= StorageDiskItem.MAX_TYPES) return 0;
		return capacity() - used;
	}

	/** Adds up to 'amount'; returns how many were added. */
	public long insert(ItemKey key, long amount, boolean simulate) {
		long n = Math.min(amount, space(key));
		if (n <= 0) return 0;
		if (!simulate) {
			items.merge(key, n, Long::sum);
			used += n;
			save();
		}
		return n;
	}

	/** Removes up to 'amount'; returns how many were removed. */
	public long extract(ItemKey key, long amount, boolean simulate) {
		Long have = items.get(key);
		if (have == null) return 0;
		long n = Math.min(amount, have);
		if (!simulate && n > 0) {
			if (have - n <= 0) items.remove(key); else items.put(key, have - n);
			used -= n;
			save();
		}
		return n;
	}

	private void save() {
		ListTag list = new ListTag();
		for (Map.Entry<ItemKey, Long> e : items.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.put("Item", e.getKey().proto().save(new CompoundTag()));
			t.putLong("Count", e.getValue());
			list.add(t);
		}
		CompoundTag tag = disk.getOrCreateTag();
		tag.put("Stored", list);
		tag.putLong("Used", used);
		tag.putInt("Types", items.size());
		onChanged.run();
		StorageNetwork.changed();
	}
}
