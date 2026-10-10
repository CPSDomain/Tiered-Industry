package com.robvanblerk.tieredpower.item.powered;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Quantum Suit modules: which piece each fits, and the "Modules" tag on the armour (module id -> switched on). */
public final class SuitModules {
	public enum Module {
		MAGNET("magnet", "Magnet", EquipmentSlot.CHEST, "Pulls dropped items and XP orbs to you from 8 blocks away"),
		FLIGHT("flight", "Flight", EquipmentSlot.CHEST, "Creative-style flight while charged (double-tap jump)"),
		AUTO_FEED("auto_feed", "Auto-Feed", EquipmentSlot.HEAD, "Eats the best food in your inventory when you get hungry"),
		JUMP_BOOST("jump_boost", "Jump Boost", EquipmentSlot.LEGS, "Jump about three blocks high"),
		WATER_WALKING("water_walking", "Water Walking", EquipmentSlot.FEET, "Walk on water (sneak to sink)"),
		RADIATION_SHIELDING("radiation_shielding", "Radiation Shielding", EquipmentSlot.CHEST, "Blocks all radiation (when radiation is switched on)"),
		AETHER_STABILIZER("aether_stabilizer", "Aether Stabilizer", EquipmentSlot.CHEST, "Flight uses a quarter of the power (Aether Crystals from the Skylands)"),
		REGENERATION("regeneration", "Regeneration", EquipmentSlot.CHEST, "Heals you over time when hurt (Lifebloom from Eden)"),
		RESISTANCE("resistance", "Resistance", EquipmentSlot.LEGS, "Takes a fifth off all damage (Amber from Redwood)"),
		ANTIDOTE("antidote", "Antidote", EquipmentSlot.HEAD, "Cures poison, wither, hunger, nausea, blindness, weakness and slowness (Witchroot from the Murk)"),
		HASTE("haste", "Haste", EquipmentSlot.HEAD, "Haste II: mine and attack faster (Gravitite from Titan)"),
		FIRE_IMMUNITY("fire_immunity", "Fire Immunity", EquipmentSlot.LEGS, "Fire and lava don't hurt you (Soul Crystals from Wraith)"),
		STEALTH("stealth", "Stealth", EquipmentSlot.FEET, "Invisible while you sneak (Umbral Shards from Umbra)");

		public final String id, title, description;
		public final EquipmentSlot slot;

		Module(String id, String title, EquipmentSlot slot, String description) {
			this.id = id;
			this.title = title;
			this.slot = slot;
			this.description = description;
		}

		public static Module byId(String id) {
			for (Module m : values()) if (m.id.equals(id)) return m;
			return null;
		}
	}

	public static boolean isQuantum(ItemStack stack) {
		return stack.getItem() instanceof QuantumArmorItem || stack.getItem() instanceof QuantumChestplateItem;
	}

	private static CompoundTag modules(ItemStack armor) {
		CompoundTag tag = armor.getTag();
		return tag == null ? new CompoundTag() : tag.getCompound("Modules");
	}

	public static boolean installed(ItemStack armor, Module m) {
		return isQuantum(armor) && modules(armor).contains(m.id);
	}

	/** Fitted, switched on, and the piece has charge. */
	public static boolean active(ItemStack armor, Module m) {
		return installed(armor, m) && modules(armor).getBoolean(m.id) && ItemEnergy.get(armor) > 0;
	}

	public static boolean switchedOn(ItemStack armor, Module m) {
		return installed(armor, m) && modules(armor).getBoolean(m.id);
	}

	public static void install(ItemStack armor, Module m) {
		CompoundTag mods = armor.getOrCreateTag().getCompound("Modules");
		mods.putBoolean(m.id, true);
		armor.getOrCreateTag().put("Modules", mods);
	}

	public static void setOn(ItemStack armor, Module m, boolean on) {
		if (!installed(armor, m)) return;
		CompoundTag mods = armor.getOrCreateTag().getCompound("Modules");
		mods.putBoolean(m.id, on);
		armor.getOrCreateTag().put("Modules", mods);
	}

	public static void remove(ItemStack armor, Module m) {
		CompoundTag tag = armor.getTag();
		if (tag == null) return;
		CompoundTag mods = tag.getCompound("Modules");
		mods.remove(m.id);
		tag.put("Modules", mods);
	}

	private SuitModules() {}
}
