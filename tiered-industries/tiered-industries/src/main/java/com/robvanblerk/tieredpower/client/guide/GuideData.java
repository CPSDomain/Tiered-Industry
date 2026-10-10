package com.robvanblerk.tieredpower.client.guide;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.TieredPower;

/** Loads assets/tieredpower/guide/guide.json - the same content as the web guide. Resource packs can replace it. */
public final class GuideData {
	public record Entry(String id, String text, List<String[]> stats, String note, String obtain, String machine) {
		public ItemStack icon() {
			Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, id));
			return new ItemStack(item);
		}

		public String name() {
			return icon().getHoverName().getString();
		}
	}

	public record Category(String id, String title, String intro, List<Entry> entries) {}

	public record Step(String title, String text) {}

	public record Book(String version, List<Step> start, List<Category> categories) {}

	private static Book cached;

	public static Book get() {
		if (cached == null) cached = load();
		return cached;
	}

	private static Book load() {
		List<Step> steps = new ArrayList<>();
		List<Category> categories = new ArrayList<>();
		String version = "";
		try {
			Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "guide/guide.json"));
			if (res.isPresent()) {
				try (Reader reader = res.get().openAsReader()) {
					JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
					version = root.has("version") ? root.get("version").getAsString() : "";
					for (JsonElement s : root.getAsJsonArray("start")) {
						JsonObject o = s.getAsJsonObject();
						steps.add(new Step(o.get("title").getAsString(), o.get("text").getAsString()));
					}
					for (JsonElement c : root.getAsJsonArray("categories")) {
						JsonObject o = c.getAsJsonObject();
						List<Entry> entries = new ArrayList<>();
						for (JsonElement e : o.getAsJsonArray("entries")) {
							JsonObject eo = e.getAsJsonObject();
							List<String[]> stats = new ArrayList<>();
							for (JsonElement st : eo.getAsJsonArray("stats")) {
								JsonArray pair = st.getAsJsonArray();
								stats.add(new String[]{pair.get(0).getAsString(), pair.get(1).getAsString()});
							}
							entries.add(new Entry(eo.get("id").getAsString(), eo.get("text").getAsString(), stats,
									str(eo, "note"), str(eo, "obtain"), str(eo, "machine")));
						}
						categories.add(new Category(o.get("id").getAsString(), o.get("title").getAsString(), str(o, "intro"), entries));
					}
				}
			}
		} catch (Exception e) {
			TieredPower.LOGGER.error("Could not load the Tiered Industries guide", e);
		}
		return new Book(version, steps, categories);
	}

	private static String str(JsonObject o, String key) {
		return o.has(key) ? o.get(key).getAsString() : "";
	}

	/** Forget the cached book (e.g. after resource packs reload). */
	public static void reset() {
		cached = null;
	}

	private GuideData() {}
}
