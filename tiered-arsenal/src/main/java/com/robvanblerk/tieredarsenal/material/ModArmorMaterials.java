package com.robvanblerk.tieredarsenal.material;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;

import java.util.function.Supplier;

/**
 * Armour materials. Defence values are listed helmet, chestplate, leggings, boots.
 *
 * Vanilla for comparison:
 *   Iron      : 2/6/5/2, toughness 0, durability x15
 *   Diamond   : 3/8/6/3, toughness 2, durability x33
 *   Netherite : 3/8/6/3, toughness 3, knockback res 0.1, durability x37
 *
 * The armour texture is looked up from the name, e.g.
 *   assets/tieredarsenal/textures/models/armor/cobalt_layer_1.png
 */
public enum ModArmorMaterials implements ArmorMaterial {
    COPPER("copper", 11, new int[]{2, 5, 4, 1}, 10, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F,
            () -> Ingredient.of(Items.COPPER_INGOT), false, false),
    EMERALD("emerald", 20, new int[]{2, 6, 5, 2}, 25, SoundEvents.ARMOR_EQUIP_DIAMOND, 0.5F, 0.0F,
            () -> Ingredient.of(Tags.Items.GEMS_EMERALD), true, false),
    COBALT("cobalt", 24, new int[]{3, 6, 5, 2}, 12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F,
            () -> Ingredient.of(ModToolTiers.INGOTS_COBALT), true, false),
    TITANIUM("titanium", 36, new int[]{3, 8, 6, 3}, 14, SoundEvents.ARMOR_EQUIP_IRON, 2.5F, 0.05F,
            () -> Ingredient.of(ModToolTiers.INGOTS_TITANIUM), true, false),
    INFERNIUM("infernium", 40, new int[]{3, 8, 7, 3}, 16, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.0F, 0.1F,
            () -> Ingredient.of(ModToolTiers.INGOTS_INFERNIUM), true, true),
    AETHERIUM("aetherium", 48, new int[]{4, 9, 7, 4}, 20, SoundEvents.ARMOR_EQUIP_NETHERITE, 4.0F, 0.15F,
            () -> Ingredient.of(ModToolTiers.INGOTS_AETHERIUM), true, true);

    /** Base durability per piece (helmet, chestplate, leggings, boots) - same as vanilla. */
    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantability;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repair;
    private final boolean hasSetBonus;
    private final boolean fireResistant;

    ModArmorMaterials(String name, int durabilityMultiplier, int[] defense, int enchantability,
                      SoundEvent equipSound, float toughness, float knockbackResistance,
                      Supplier<Ingredient> repair, boolean hasSetBonus, boolean fireResistant) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantability = enchantability;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repair = repair;
        this.hasSetBonus = hasSetBonus;
        this.fireResistant = fireResistant;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }

    @Override
    public String getName() {
        return TieredArsenal.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    /** Short name without the mod id, e.g. "cobalt". */
    public String id() {
        return name;
    }

    public boolean hasSetBonus() {
        return hasSetBonus;
    }

    public boolean isFireResistant() {
        return fireResistant;
    }
}
