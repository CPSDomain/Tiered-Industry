package com.robvanblerk.tieredarsenal.module;

import com.robvanblerk.tieredarsenal.item.TieredBowItem;
import com.robvanblerk.tieredarsenal.item.TieredCrossbowItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

/**
 * Modules fitted to gear at the Armoury Workbench.
 * Each module can be fitted once per piece; the number of slots depends on the tier.
 */
public enum GearModule {
    // Weapons (swords, axes, greatswords, daggers, spears, war hammers, bows, crossbows)
    LIFESTEAL(Fits.WEAPON),       // heals a share of damage dealt
    FIRE(Fits.WEAPON),            // sets the target on fire for 4 s
    KNOCKBACK(Fits.WEAPON),       // +1 attack knockback
    LOOTING(Fits.WEAPON),         // +1 looting level
    // Armour
    VITALITY(Fits.ARMOUR),        // +2 hearts per piece
    BULWARK(Fits.ARMOUR),         // +10% knockback resistance per piece
    STEP_ASSIST(Fits.BOOTS),      // step up full blocks
    AQUA_LUNG(Fits.HELMET),       // water breathing while under water
    THORNS(Fits.ARMOUR),          // 10% of melee damage taken goes back, per piece
    REGENERATION(Fits.CHESTPLATE),// Regeneration I while hurt
    SWIFTNESS(Fits.LEGGINGS),     // +10% walking speed
    // Tools (pickaxes, shovels, axes, hoes)
    AUTO_SMELT(Fits.TOOL),        // blocks drop their smelted result
    VEIN_MINER(Fits.TOOL),        // sneak-mine a whole ore vein or tree
    // Boss reward (needs a Guardian Core)
    CELESTIAL(Fits.GEAR);         // weapons +2 damage; armour +1 armour and +1 toughness

    public enum Fits { WEAPON, ARMOUR, BOOTS, HELMET, CHESTPLATE, LEGGINGS, TOOL, GEAR }

    private final Fits fits;

    GearModule(Fits fits) {
        this.fits = fits;
    }

    public Fits fits() {
        return fits;
    }

    /** Lower-case id used in NBT, item names and lang keys, e.g. "step_assist". */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean canFit(ItemStack gear) {
        var item = gear.getItem();
        boolean weapon = item instanceof SwordItem || item instanceof AxeItem
                || item instanceof TieredBowItem || item instanceof TieredCrossbowItem;
        boolean tool = item instanceof DiggerItem;
        ArmorItem armour = item instanceof ArmorItem a ? a : null;
        return switch (fits) {
            case WEAPON -> weapon;
            case TOOL -> tool;
            case GEAR -> weapon || armour != null;
            case ARMOUR -> armour != null;
            case BOOTS -> armour != null && armour.getEquipmentSlot() == EquipmentSlot.FEET;
            case HELMET -> armour != null && armour.getEquipmentSlot() == EquipmentSlot.HEAD;
            case CHESTPLATE -> armour != null && armour.getEquipmentSlot() == EquipmentSlot.CHEST;
            case LEGGINGS -> armour != null && armour.getEquipmentSlot() == EquipmentSlot.LEGS;
        };
    }

    public static GearModule byId(String id) {
        for (GearModule m : values()) {
            if (m.id().equals(id)) return m;
        }
        return null;
    }
}
