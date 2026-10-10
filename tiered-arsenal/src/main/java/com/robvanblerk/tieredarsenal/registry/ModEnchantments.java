package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.enchant.ArsenalEnchantment;
import com.robvanblerk.tieredarsenal.item.DaggerItem;
import com.robvanblerk.tieredarsenal.item.SpearItem;
import com.robvanblerk.tieredarsenal.item.WarHammerItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Enchantments for the new weapon types:
 *   Shockwave I-III (war hammer) - +0.5 blocks radius and +10% splash damage per level
 *   Assassin I-III  (dagger)     - +0.25x backstab damage per level
 *   Reach I-II      (spear)      - +0.5 blocks reach per level
 *   Counter I-III   (any shield) - blocking a melee hit deals 2 damage per level to the attacker
 */
public final class ModEnchantments {
    private ModEnchantments() {}

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, TieredArsenal.MOD_ID);

    public static final EnchantmentCategory WARHAMMER =
            EnchantmentCategory.create("TIEREDARSENAL_WARHAMMER", item -> item instanceof WarHammerItem);
    public static final EnchantmentCategory DAGGER =
            EnchantmentCategory.create("TIEREDARSENAL_DAGGER", item -> item instanceof DaggerItem);
    public static final EnchantmentCategory SPEAR =
            EnchantmentCategory.create("TIEREDARSENAL_SPEAR", item -> item instanceof SpearItem);
    public static final EnchantmentCategory SHIELD =
            EnchantmentCategory.create("TIEREDARSENAL_SHIELD", item -> item instanceof ShieldItem);

    public static final RegistryObject<Enchantment> SHOCKWAVE = ENCHANTMENTS.register("shockwave",
            () -> new ArsenalEnchantment(Enchantment.Rarity.UNCOMMON, WARHAMMER, 3, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> ASSASSIN = ENCHANTMENTS.register("assassin",
            () -> new ArsenalEnchantment(Enchantment.Rarity.UNCOMMON, DAGGER, 3, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> REACH = ENCHANTMENTS.register("reach",
            () -> new ArsenalEnchantment(Enchantment.Rarity.RARE, SPEAR, 2, EquipmentSlot.MAINHAND));
    public static final RegistryObject<Enchantment> COUNTER = ENCHANTMENTS.register("counter",
            () -> new ArsenalEnchantment(Enchantment.Rarity.UNCOMMON, SHIELD, 3,
                    EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));

    public static int level(RegistryObject<Enchantment> enchantment, ItemStack stack) {
        return stack.isEmpty() ? 0 : EnchantmentHelper.getItemEnchantmentLevel(enchantment.get(), stack);
    }
}
