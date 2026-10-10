package com.robvanblerk.tieredpower.item.powered;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Tool tiers for the powered tools. 0 uses = they never wear out; they run on energy instead. */
public enum PoweredTier implements Tier {
	ELECTRIC(3, 10.0f, 3.0f),   // mines like diamond
	ADVANCED(4, 16.0f, 4.0f),   // mines like netherite
	QUANTUM(4, 28.0f, 7.0f);    // netherite level, much faster

	private final int level;
	private final float speed;
	private final float damage;

	PoweredTier(int level, float speed, float damage) {
		this.level = level;
		this.speed = speed;
		this.damage = damage;
	}

	@Override
	public int getUses() {
		return 0;
	}

	@Override
	public float getSpeed() {
		return speed;
	}

	@Override
	public float getAttackDamageBonus() {
		return damage;
	}

	@Override
	public int getLevel() {
		return level;
	}

	@Override
	public int getEnchantmentValue() {
		return 10;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.EMPTY;
	}
}
