package com.robvanblerk.tieredpower.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import com.robvanblerk.tieredpower.item.FluidDiskItem;

/** The contents of one Fluid Storage Disk (fluids and gases, in mB). Like DiskInventory, the disk's NBT is the real copy. */
public class FluidDiskInventory {
	private final ItemStack disk;
	private final FluidDiskItem type;
	private final Runnable onChanged;
	private final Map<FluidKey, Long> fluids = new LinkedHashMap<>();
	private long used;

	public FluidDiskInventory(ItemStack disk, Runnable onChanged) {
		this.disk = disk;
		this.type = (FluidDiskItem) disk.getItem();
		this.onChanged = onChanged;
		CompoundTag tag = disk.getTag();
		if (tag != null && tag.contains("StoredFluids", Tag.TAG_LIST)) {
			ListTag list = tag.getList("StoredFluids", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag e = list.getCompound(i);
				FluidStack proto = FluidStack.loadFluidStackFromNBT(e.getCompound("Fluid"));
				long amount = e.getLong("Amount");
				if (proto.isEmpty() || amount <= 0) continue;
				fluids.merge(new FluidKey(proto), amount, Long::sum);
				used += amount;
			}
		}
	}

	public ItemStack disk() { return disk; }

	private java.util.Set<FluidKey> partition;

	public java.util.Set<FluidKey> partition() {
		if (partition == null) {
			partition = new java.util.HashSet<>();
			for (FluidStack f : DiskConfig.fluidPartition(disk)) partition.add(new FluidKey(f));
		}
		return partition;
	}

	public boolean isPartitioned() { return !partition().isEmpty(); }
	public boolean accepts(FluidKey key) { return partition().isEmpty() || partition().contains(key); }
	public int priority() { return DiskConfig.priority(disk); }
	public Map<FluidKey, Long> fluids() { return fluids; }
	public long used() { return used; }
	public long capacity() { return type.getCapacityMb(); }

	public long space(FluidKey key) {
		if (!accepts(key)) return 0;
		if (!fluids.containsKey(key) && fluids.size() >= FluidDiskItem.MAX_TYPES) return 0;
		return capacity() - used;
	}

	public long insert(FluidKey key, long amount, boolean simulate) {
		long n = Math.min(amount, space(key));
		if (n <= 0) return 0;
		if (!simulate) {
			fluids.merge(key, n, Long::sum);
			used += n;
			save();
		}
		return n;
	}

	public long extract(FluidKey key, long amount, boolean simulate) {
		Long have = fluids.get(key);
		if (have == null) return 0;
		long n = Math.min(amount, have);
		if (!simulate && n > 0) {
			if (have - n <= 0) fluids.remove(key); else fluids.put(key, have - n);
			used -= n;
			save();
		}
		return n;
	}

	private void save() {
		ListTag list = new ListTag();
		for (Map.Entry<FluidKey, Long> e : fluids.entrySet()) {
			CompoundTag t = new CompoundTag();
			t.put("Fluid", e.getKey().proto().writeToNBT(new CompoundTag()));
			t.putLong("Amount", e.getValue());
			list.add(t);
		}
		CompoundTag tag = disk.getOrCreateTag();
		tag.put("StoredFluids", list);
		tag.putLong("Used", used);
		tag.putInt("Types", fluids.size());
		onChanged.run();
		StorageNetwork.changed();
	}
}
