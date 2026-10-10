package com.robvanblerk.tieredarsenal.enchant;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/** A simple enchantment with a level cap, found on the enchanting table and on books. */
public class ArsenalEnchantment extends Enchantment {
    private final int maxLevel;

    public ArsenalEnchantment(Rarity rarity, EnchantmentCategory category, int maxLevel, EquipmentSlot... slots) {
        super(rarity, category, slots);
        this.maxLevel = maxLevel;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 9;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 25;
    }
}
