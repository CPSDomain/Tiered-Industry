package com.robvanblerk.tieredpower.radiation;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** The Hazmat Suit: little armour, but each piece blocks a quarter of incoming radiation. */
public enum HazmatMaterial implements ArmorMaterial {
	INSTANCE;

	@Override
	public int getDurabilityForType(ArmorItem.Type type) {
		return switch (type) { case HELMET -> 110; case CHESTPLATE -> 160; case LEGGINGS -> 150; case BOOTS -> 130; };
	}

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return switch (type) { case HELMET -> 1; case CHESTPLATE -> 3; case LEGGINGS -> 2; case BOOTS -> 1; };
	}

	@Override public int getEnchantmentValue() { return 10; }
	@Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
	@Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.LEATHER); }
	@Override public String getName() { return "tieredpower:hazmat"; }
	@Override public float getToughness() { return 0; }
	@Override public float getKnockbackResistance() { return 0; }
}
