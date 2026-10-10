package com.robvanblerk.tieredpower.stargate;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Where each planet's arrival gate stands (built the first time anyone goes there), and each planet's "home gate":
 * the gate back home that last dialled it, which the planet's Home address (and redstone dialling) leads to.
 */
public class StargateData extends SavedData {
	private final Map<String, BlockPos> arrivals = new HashMap<>();
	/** planet id -> home gate's ring (bottom middle) and where to step out in front of it. */
	private final Map<String, GlobalPos> homeGates = new HashMap<>();
	private final Map<String, BlockPos> homeSpots = new HashMap<>();
	/** Hidden worlds each player has found the address of. */
	private final Map<java.util.UUID, java.util.Set<String>> learned = new HashMap<>();

	public boolean knows(java.util.UUID player, Planet p) {
		return p.known || learned.getOrDefault(player, java.util.Set.of()).contains(p.id);
	}

	public void learn(java.util.UUID player, Planet p) {
		if (p.known) return;
		if (learned.computeIfAbsent(player, k -> new java.util.HashSet<>()).add(p.id)) setDirty();
	}

	/** Bit i set = the player knows Planet i's address. */
	public int knownMask(java.util.UUID player) {
		int m = 0;
		for (Planet p : Planet.values()) if (knows(player, p)) m |= 1 << p.ordinal();
		return m;
	}

	public static StargateData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(StargateData::load, StargateData::new, "tieredpower_stargates");
	}

	public @Nullable BlockPos arrival(Planet p) {
		return arrivals.get(p.id);
	}

	public void setArrival(Planet p, BlockPos pos) {
		arrivals.put(p.id, pos);
		setDirty();
	}

	public @Nullable GlobalPos homeGate(Planet p) { return homeGates.get(p.id); }
	public @Nullable BlockPos homeSpot(Planet p) { return homeSpots.get(p.id); }

	public void setHomeGate(Planet p, GlobalPos gate, BlockPos spot) {
		homeGates.put(p.id, gate);
		homeSpots.put(p.id, spot.immutable());
		setDirty();
	}

	public static StargateData load(CompoundTag tag) {
		StargateData d = new StargateData();
		for (String k : tag.getAllKeys()) if (!k.equals("homes") && !k.equals("learned")) d.arrivals.put(k, BlockPos.of(tag.getLong(k)));
		CompoundTag homes = tag.getCompound("homes");
		for (String k : homes.getAllKeys()) {
			CompoundTag h = homes.getCompound(k);
			ResourceLocation dim = ResourceLocation.tryParse(h.getString("dim"));
			if (dim == null) continue;
			d.homeGates.put(k, GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(h.getLong("gate"))));
			d.homeSpots.put(k, BlockPos.of(h.getLong("spot")));
		}
		CompoundTag l = tag.getCompound("learned");
		for (String k : l.getAllKeys()) {
			try {
				java.util.Set<String> set = new java.util.HashSet<>();
				for (net.minecraft.nbt.Tag t : l.getList(k, net.minecraft.nbt.Tag.TAG_STRING)) set.add(t.getAsString());
				d.learned.put(java.util.UUID.fromString(k), set);
			} catch (IllegalArgumentException ignored) {}
		}
		return d;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		for (var e : arrivals.entrySet()) tag.putLong(e.getKey(), e.getValue().asLong());
		CompoundTag homes = new CompoundTag();
		for (var e : homeGates.entrySet()) {
			CompoundTag h = new CompoundTag();
			h.putString("dim", e.getValue().dimension().location().toString());
			h.putLong("gate", e.getValue().pos().asLong());
			h.putLong("spot", homeSpots.getOrDefault(e.getKey(), e.getValue().pos()).asLong());
			homes.put(e.getKey(), h);
		}
		tag.put("homes", homes);
		CompoundTag l = new CompoundTag();
		for (var e : learned.entrySet()) {
			net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
			for (String id : e.getValue()) list.add(net.minecraft.nbt.StringTag.valueOf(id));
			l.put(e.getKey().toString(), list);
		}
		tag.put("learned", l);
		return tag;
	}
}
