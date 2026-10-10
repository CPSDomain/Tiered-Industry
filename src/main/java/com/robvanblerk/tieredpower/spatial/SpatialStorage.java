package com.robvanblerk.tieredpower.spatial;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Where captured spaces live (kept with the world, not on the cell item, so cells stay small). */
public class SpatialStorage extends SavedData {
	private final Map<UUID, CompoundTag> spaces = new HashMap<>();

	public static SpatialStorage get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(SpatialStorage::load, SpatialStorage::new, "tieredpower_spatial");
	}

	public @Nullable CompoundTag get(UUID id) {
		return spaces.get(id);
	}

	public void put(UUID id, CompoundTag data) {
		spaces.put(id, data);
		setDirty();
	}

	public void remove(UUID id) {
		if (spaces.remove(id) != null) setDirty();
	}

	public static SpatialStorage load(CompoundTag tag) {
		SpatialStorage s = new SpatialStorage();
		for (String key : tag.getAllKeys()) {
			try {
				s.spaces.put(UUID.fromString(key), tag.getCompound(key));
			} catch (IllegalArgumentException ignored) {}
		}
		return s;
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		for (var e : spaces.entrySet()) tag.put(e.getKey().toString(), e.getValue());
		return tag;
	}
}
