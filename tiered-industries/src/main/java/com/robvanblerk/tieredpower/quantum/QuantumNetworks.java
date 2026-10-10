package com.robvanblerk.tieredpower.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The quantum link between chests: where each main Quantum Chest is, and the items "in transit" to a main chest whose
 * chunk isn't loaded right now (they land in it as soon as it is). Saved with the world.
 */
public class QuantumNetworks extends SavedData {
	/** How many stacks can wait for a main chest that isn't loaded. */
	public static final int PENDING_STACKS = 270;

	private final Map<UUID, GlobalPos> mains = new HashMap<>();
	private final Map<UUID, List<ItemStack>> pending = new HashMap<>();

	public static QuantumNetworks get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(QuantumNetworks::load, QuantumNetworks::new, "tieredpower_quantum_chests");
	}

	public void setMain(UUID id, GlobalPos pos) {
		if (!pos.equals(mains.get(id))) {
			mains.put(id, pos);
			setDirty();
		}
	}

	public @Nullable GlobalPos main(UUID id) {
		return mains.get(id);
	}

	/** A main chest was broken: forget it and hand back what was waiting for it. */
	public List<ItemStack> removeMain(UUID id) {
		mains.remove(id);
		List<ItemStack> left = pending.remove(id);
		setDirty();
		return left == null ? List.of() : left;
	}

	/** Queues items for a main chest that isn't loaded. Returns what didn't fit. */
	public ItemStack queue(UUID id, ItemStack stack) {
		List<ItemStack> list = pending.computeIfAbsent(id, k -> new ArrayList<>());
		ItemStack left = stack.copy();
		for (ItemStack s : list) {
			if (left.isEmpty()) break;
			if (ItemHandlerHelper.canItemStacksStack(s, left)) {
				int n = Math.min(left.getCount(), s.getMaxStackSize() - s.getCount());
				s.grow(n);
				left.shrink(n);
			}
		}
		while (!left.isEmpty() && list.size() < PENDING_STACKS) {
			int n = Math.min(left.getCount(), left.getMaxStackSize());
			list.add(left.split(n));
		}
		if (left.getCount() != stack.getCount()) setDirty();
		return left;
	}

	/** Items waiting for this main chest (live list: take from it). */
	public List<ItemStack> pending(UUID id) {
		return pending.getOrDefault(id, List.of());
	}

	public int pendingCount(UUID id) {
		int n = 0;
		for (ItemStack s : pending(id)) n += s.getCount();
		return n;
	}

	public void changed() {
		setDirty();
	}

	public static QuantumNetworks load(CompoundTag tag) {
		QuantumNetworks d = new QuantumNetworks();
		CompoundTag mains = tag.getCompound("mains");
		for (String k : mains.getAllKeys()) {
			try {
				CompoundTag m = mains.getCompound(k);
				ResourceLocation dim = ResourceLocation.tryParse(m.getString("dim"));
				if (dim != null) d.mains.put(UUID.fromString(k), GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(m.getLong("pos"))));
			} catch (IllegalArgumentException ignored) {}
		}
		CompoundTag pend = tag.getCompound("pending");
		for (String k : pend.getAllKeys()) {
			try {
				List<ItemStack> list = new ArrayList<>();
				for (Tag t : pend.getList(k, Tag.TAG_COMPOUND)) list.add(ItemStack.of((CompoundTag) t));
				d.pending.put(UUID.fromString(k), list);
			} catch (IllegalArgumentException ignored) {}
		}
		return d;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		CompoundTag m = new CompoundTag();
		for (var e : mains.entrySet()) {
			CompoundTag c = new CompoundTag();
			c.putString("dim", e.getValue().dimension().location().toString());
			c.putLong("pos", e.getValue().pos().asLong());
			m.put(e.getKey().toString(), c);
		}
		tag.put("mains", m);
		CompoundTag p = new CompoundTag();
		for (var e : pending.entrySet()) {
			ListTag list = new ListTag();
			for (ItemStack s : e.getValue()) if (!s.isEmpty()) list.add(s.save(new CompoundTag()));
			if (!list.isEmpty()) p.put(e.getKey().toString(), list);
		}
		tag.put("pending", p);
		return tag;
	}
}
