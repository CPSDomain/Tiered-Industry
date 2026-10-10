package com.robvanblerk.tieredpower.item.powered;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Armour material for the jetpacks: no durability (they run on energy), some protection. */
public enum JetpackMaterial implements ArmorMaterial {
	BASIC("tieredpower:jetpack", 3),
	ADVANCED("tieredpower:advanced_jetpack", 6);

	private final String name;
	private final int defense;

	JetpackMaterial(String name, int defense) {
		this.name = name;
		this.defense = defense;
	}

	@Override
	public int getDurabilityForType(ArmorItem.Type type) {
		return 0;
	}

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return defense;
	}

	@Override
	public int getEnchantmentValue() {
		return 10;
	}

	@Override
	public SoundEvent getEquipSound() {
		return SoundEvents.ARMOR_EQUIP_IRON;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.EMPTY;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public float getToughness() {
		return this == ADVANCED ? 1.0f : 0.0f;
	}

	@Override
	public float getKnockbackResistance() {
		return 0;
	}
}
