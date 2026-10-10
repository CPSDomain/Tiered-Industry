package com.robvanblerk.tieredpower.space;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every satellite in orbit (saved with the world): who launched it, and which Receiver Dish has claimed it, if any. */
public class SatelliteData extends SavedData {
	public static final class Satellite {
		public final int id;
		public final UUID owner;
		public final String ownerName;
		/** "solar" (Receiver Dish) or "mining" (Laser Drill). */
		public final String type;
		public @Nullable BlockPos dish;
		public @Nullable String dishDim;

		Satellite(int id, UUID owner, String ownerName, String type) {
			this.id = id;
			this.owner = owner;
			this.ownerName = ownerName;
			this.type = type;
		}
	}

	private final List<Satellite> satellites = new ArrayList<>();
	private int nextId = 1;

	public static SatelliteData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SatelliteData::load, SatelliteData::new, "tieredpower_satellites");
	}

	public List<Satellite> all() {
		return satellites;
	}

	public Satellite launch(UUID owner, String ownerName, String type) {
		Satellite s = new Satellite(nextId++, owner, ownerName, type);
		satellites.add(s);
		setDirty();
		return s;
	}

	public @Nullable Satellite forDish(String dim, BlockPos pos) {
		for (Satellite s : satellites) if (pos.equals(s.dish) && dim.equals(s.dishDim)) return s;
		return null;
	}

	/** Gives this dish one of the player's unclaimed satellites. */
	public @Nullable Satellite claim(String dim, BlockPos pos, UUID player, String type) {
		if (forDish(dim, pos) != null) return forDish(dim, pos);
		for (Satellite s : satellites) {
			if (s.dish == null && s.owner.equals(player) && s.type.equals(type)) {
				s.dish = pos.immutable();
				s.dishDim = dim;
				setDirty();
				return s;
			}
		}
		return null;
	}

	public void release(String dim, BlockPos pos) {
		Satellite s = forDish(dim, pos);
		if (s != null) {
			s.dish = null;
			s.dishDim = null;
			setDirty();
		}
	}

	public int claimed() {
		int n = 0;
		for (Satellite s : satellites) if (s.dish != null) n++;
		return n;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Satellite s : satellites) {
			CompoundTag t = new CompoundTag();
			t.putInt("id", s.id);
			t.putUUID("owner", s.owner);
			t.putString("ownerName", s.ownerName);
			t.putString("type", s.type);
			if (s.dish != null && s.dishDim != null) {
				t.putLong("dish", s.dish.asLong());
				t.putString("dishDim", s.dishDim);
			}
			list.add(t);
		}
		tag.put("satellites", list);
		tag.putInt("nextId", nextId);
		return tag;
	}

	public static SatelliteData load(CompoundTag tag) {
		SatelliteData d = new SatelliteData();
		d.nextId = Math.max(1, tag.getInt("nextId"));
		ListTag list = tag.getList("satellites", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			Satellite s = new Satellite(t.getInt("id"), t.getUUID("owner"), t.getString("ownerName"), t.contains("type") ? t.getString("type") : "solar");
			if (t.contains("dish")) {
				s.dish = BlockPos.of(t.getLong("dish"));
				s.dishDim = t.getString("dishDim");
			}
			d.satellites.add(s);
		}
		return d;
	}
}
