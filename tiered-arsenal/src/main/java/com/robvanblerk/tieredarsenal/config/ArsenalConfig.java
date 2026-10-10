package com.robvanblerk.tieredarsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Balance settings, saved to config/tieredarsenal-common.toml.
 * Changes take effect after a restart.
 */
public final class ArsenalConfig {
    private ArsenalConfig() {}

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SET_BONUSES;
    public static final ForgeConfigSpec.DoubleValue DAGGER_BACKSTAB;
    public static final ForgeConfigSpec.DoubleValue WARHAMMER_SPLASH;
    public static final ForgeConfigSpec.DoubleValue SPEAR_THROW_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIFESTEAL;
    public static final ForgeConfigSpec.DoubleValue AETHERIUM_SHIELD_REFLECT;
    public static final ForgeConfigSpec.BooleanValue CHEST_LOOT;
    public static final ForgeConfigSpec.DoubleValue MOB_GEAR_CHANCE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Armour").push("armour");
        SET_BONUSES = b.comment("Give full-set bonuses (Haste, Speed, Fire Resistance, ...) for wearing four pieces of one material.")
                .define("setBonuses", true);
        b.pop();

        b.comment("Weapons").push("weapons");
        DAGGER_BACKSTAB = b.comment("Damage multiplier when a dagger hits a mob from behind.")
                .defineInRange("daggerBackstabMultiplier", 1.5, 1.0, 5.0);
        WARHAMMER_SPLASH = b.comment("Share of a war hammer hit dealt to mobs around the target (0.4 = 40%).")
                .defineInRange("warHammerSplash", 0.4, 0.0, 1.0);
        SPEAR_THROW_MULTIPLIER = b.comment("Thrown spear damage as a multiple of its melee damage.")
                .defineInRange("spearThrowMultiplier", 1.25, 0.1, 5.0);
        AETHERIUM_SHIELD_REFLECT = b.comment("Share of blocked melee damage the Aetherium Shield reflects (0.25 = 25%).")
                .defineInRange("aetheriumShieldReflect", 0.25, 0.0, 2.0);
        b.pop();

        b.comment("Gear modules").push("modules");
        LIFESTEAL = b.comment("Share of damage dealt that the Lifesteal module heals (0.15 = 15%).")
                .defineInRange("lifesteal", 0.15, 0.0, 1.0);
        b.pop();

        b.comment("World").push("world");
        CHEST_LOOT = b.comment("Add Tiered Arsenal ingots, templates and modules to dungeon, bastion, End city and other chests.")
                .define("chestLoot", true);
        MOB_GEAR_CHANCE = b.comment("Chance a zombie or skeleton spawns wearing a piece of Tiered Arsenal gear (0.03 = 3%; 0 turns it off).")
                .defineInRange("mobGearChance", 0.03, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }
}
