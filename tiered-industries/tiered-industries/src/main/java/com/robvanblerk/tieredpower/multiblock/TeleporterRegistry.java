package com.robvanblerk.tieredpower.multiblock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Every Teleporter pad in the world (all dimensions), with its name. Saved with the world. */
public class TeleporterRegistry extends SavedData {
	public record Pad(ResourceLocation dim, BlockPos pos, String name) {
		public String key() {
			return key(dim, pos);
		}

		public static String key(ResourceLocation dim, BlockPos pos) {
			return dim + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
		}
	}

	private final Map<String, Pad> pads = new LinkedHashMap<>();

	public static TeleporterRegistry get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TeleporterRegistry::load, TeleporterRegistry::new, "tieredpower_teleporters");
	}

	public void put(ResourceLocation dim, BlockPos pos, String name) {
		pads.put(Pad.key(dim, pos), new Pad(dim, pos.immutable(), name));
		setDirty();
	}

	public void remove(ResourceLocation dim, BlockPos pos) {
		if (pads.remove(Pad.key(dim, pos)) != null) setDirty();
	}

	public Pad find(ResourceLocation dim, BlockPos pos) {
		return pads.get(Pad.key(dim, pos));
	}

	public List<Pad> all() {
		return new ArrayList<>(pads.values());
	}

	@Override
	public CompoundTag save(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Pad p : pads.values()) {
			CompoundTag t = new CompoundTag();
			t.putString("dim", p.dim().toString());
			t.put("pos", NbtUtils.writeBlockPos(p.pos()));
			t.putString("name", p.name());
			list.add(t);
		}
		tag.put("pads", list);
		return tag;
	}

	public static TeleporterRegistry load(CompoundTag tag) {
		TeleporterRegistry r = new TeleporterRegistry();
		ListTag list = tag.getList("pads", 10);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			ResourceLocation dim = ResourceLocation.tryParse(t.getString("dim"));
			if (dim == null) continue;
			BlockPos pos = NbtUtils.readBlockPos(t.getCompound("pos"));
			r.pads.put(Pad.key(dim, pos), new Pad(dim, pos, t.getString("name")));
		}
		return r;
	}
}
