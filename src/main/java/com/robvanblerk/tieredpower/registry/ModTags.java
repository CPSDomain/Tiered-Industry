package com.robvanblerk.tieredpower.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** Shared Forge tags, so lithium from other mods (e.g. Mekanism) works in our machines too. */
public final class ModTags {
	/** Plant and animal matter a Bio-Digester turns into methane. */
	public static final TagKey<Item> BIOMASS =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("tieredpower", "biomass"));

	public static final TagKey<Item> SALT =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "dusts/salt"));

	public static final TagKey<Item> LITHIUM_INGOTS =
			TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ingots/lithium"));

	private ModTags() {}
}
