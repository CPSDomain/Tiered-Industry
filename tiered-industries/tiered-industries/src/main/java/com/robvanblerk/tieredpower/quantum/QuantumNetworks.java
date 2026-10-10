package com.robvanblerk.tieredpower.quantum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * The quantum link between chests, saved with the world: where each main Quantum Chest is, every linked chest (its
 * main chest, Send/Receive mode and filter), and items "in transit" to a chest whose area isn't loaded right now.
 */
public class QuantumNetworks extends SavedData {
	/** How many stacks can wait for a main chest that isn't loaded. */
	public static final int MAIN_PENDING = 270;
	/** How many stacks can wait for a Receive chest that isn't loaded. */
	public static final int RECEIVE_PENDING = 54;

	/** A linked chest as seen from its main chest. */
	public record Link(UUID main, boolean receive, List<Item> filter, GlobalPos pos) {
		public boolean matches(ItemStack stack) {
			return filter.contains(stack.getItem());
		}
	}

	private final Map<UUID, GlobalPos> mains = new HashMap<>();
	private final Map<UUID, Link> links = new HashMap<>();
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

	public void setLink(UUID chest, @Nullable Link link) {
		if (link == null) {
			if (links.remove(chest) != null) setDirty();
		} else if (!link.equals(links.get(chest))) {
			links.put(chest, link);
			setDirty();
		}
	}

	/** The Receive chests linked to this main chest. */
	public List<Map.Entry<UUID, Link>> receiversOf(UUID main) {
		List<Map.Entry<UUID, Link>> out = new ArrayList<>();
		for (var e : links.entrySet()) if (e.getValue().receive() && e.getValue().main().equals(main)) out.add(e);
		return out;
	}

	/** {sending, receiving} chests linked to this main chest. */
	public int[] linkCounts(UUID main) {
		int send = 0, receive = 0;
		for (Link l : links.values()) {
			if (!l.main().equals(main)) continue;
			if (l.receive()) receive++; else send++;
		}
		return new int[] { send, receive };
	}

	/** A chest was broken: forget it and hand back what was in transit to it. */
	public List<ItemStack> removeChest(UUID id) {
		mains.remove(id);
		links.remove(id);
		List<ItemStack> left = pending.remove(id);
		setDirty();
		return left == null ? List.of() : left;
	}

	/** Queues items for a chest that isn't loaded, up to 'cap' stacks. Returns what didn't fit. */
	public ItemStack queue(UUID dest, ItemStack stack, int cap) {
		List<ItemStack> list = pending.computeIfAbsent(dest, k -> new ArrayList<>());
		ItemStack left = stack.copy();
		for (ItemStack s : list) {
			if (left.isEmpty()) break;
			if (ItemHandlerHelper.canItemStacksStack(s, left)) {
				int n = Math.min(left.getCount(), s.getMaxStackSize() - s.getCount());
				s.grow(n);
				left.shrink(n);
			}
		}
		while (!left.isEmpty() && list.size() < cap) {
			int n = Math.min(left.getCount(), left.getMaxStackSize());
			list.add(left.split(n));
		}
		if (left.getCount() != stack.getCount()) setDirty();
		return left;
	}

	/** Items in transit to this chest (live list: take from it, then call changed()). */
	public List<ItemStack> pending(UUID id) {
		return pending.getOrDefault(id, List.of());
	}

	/** Takes everything in transit to this chest (used when its link changes). */
	public List<ItemStack> takePending(UUID id) {
		List<ItemStack> left = pending.remove(id);
		if (left != null) setDirty();
		return left == null ? List.of() : left;
	}

	public int pendingCount(UUID id) {
		int n = 0;
		for (ItemStack s : pending(id)) n += s.getCount();
		return n;
	}

	public void changed() {
		setDirty();
	}

	private static @Nullable GlobalPos readPos(CompoundTag c) {
		ResourceLocation dim = ResourceLocation.tryParse(c.getString("dim"));
		return dim == null ? null : GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(c.getLong("pos")));
	}

	private static void writePos(CompoundTag c, GlobalPos pos) {
		c.putString("dim", pos.dimension().location().toString());
		c.putLong("pos", pos.pos().asLong());
	}

	public static QuantumNetworks load(CompoundTag tag) {
		QuantumNetworks d = new QuantumNetworks();
		CompoundTag mains = tag.getCompound("mains");
		for (String k : mains.getAllKeys()) {
			try {
				GlobalPos pos = readPos(mains.getCompound(k));
				if (pos != null) d.mains.put(UUID.fromString(k), pos);
			} catch (IllegalArgumentException ignored) {}
		}
		CompoundTag links = tag.getCompound("links");
		for (String k : links.getAllKeys()) {
			try {
				CompoundTag c = links.getCompound(k);
				GlobalPos pos = readPos(c);
				if (pos == null || !c.hasUUID("main")) continue;
				List<Item> filter = new ArrayList<>();
				for (Tag t : c.getList("filter", Tag.TAG_STRING)) {
					ResourceLocation rl = ResourceLocation.tryParse(t.getAsString());
					Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
					if (item != Items.AIR) filter.add(item);
				}
				d.links.put(UUID.fromString(k), new Link(c.getUUID("main"), c.getBoolean("receive"), filter, pos));
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
			writePos(c, e.getValue());
			m.put(e.getKey().toString(), c);
		}
		tag.put("mains", m);
		CompoundTag l = new CompoundTag();
		for (var e : links.entrySet()) {
			CompoundTag c = new CompoundTag();
			Link link = e.getValue();
			c.putUUID("main", link.main());
			c.putBoolean("receive", link.receive());
			writePos(c, link.pos());
			ListTag filter = new ListTag();
			for (Item item : link.filter()) filter.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(item).toString()));
			c.put("filter", filter);
			l.put(e.getKey().toString(), c);
		}
		tag.put("links", l);
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
