package com.robvanblerk.tieredpower.item.powered;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** The Quantum Suit: tougher than netherite, and never wears out (it runs on energy instead). */
public enum QuantumArmorMaterial implements ArmorMaterial {
	INSTANCE;

	@Override public int getDurabilityForType(ArmorItem.Type type) { return 0; }

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return switch (type) {
			case HELMET -> 4;
			case CHESTPLATE -> 9;
			case LEGGINGS -> 7;
			case BOOTS -> 4;
		};
	}

	@Override public int getEnchantmentValue() { return 15; }
	@Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
	@Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
	@Override public String getName() { return "tieredpower:quantum"; }
	@Override public float getToughness() { return 4.0f; }
	@Override public float getKnockbackResistance() { return 0.15f; }
}
