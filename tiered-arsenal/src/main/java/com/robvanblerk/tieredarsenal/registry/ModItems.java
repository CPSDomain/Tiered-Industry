package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.item.DaggerItem;
import com.robvanblerk.tieredarsenal.item.ModuleItem;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.item.SpearItem;
import com.robvanblerk.tieredarsenal.item.TieredArmorItem;
import com.robvanblerk.tieredarsenal.item.TieredBowItem;
import com.robvanblerk.tieredarsenal.item.TieredShieldItem;
import com.robvanblerk.tieredarsenal.item.TieredCrossbowItem;
import com.robvanblerk.tieredarsenal.item.TieredArrowItem;
import com.robvanblerk.tieredarsenal.item.QuiverItem;
import com.robvanblerk.tieredarsenal.item.ToolBeltItem;
import com.robvanblerk.tieredarsenal.item.VoidSigilItem;
import com.robvanblerk.tieredarsenal.item.AnnihilatorItem;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import java.util.function.Supplier;
import com.robvanblerk.tieredarsenal.item.UpgradeTemplates;
import com.robvanblerk.tieredarsenal.item.WarHammerItem;
import com.robvanblerk.tieredarsenal.material.ModArmorMaterials;
import com.robvanblerk.tieredarsenal.material.ModToolTiers;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TieredArsenal.MOD_ID);

    // ---- Materials from the new ores ----
    public static final RegistryObject<Item> RAW_COBALT = simple("raw_cobalt", false);
    public static final RegistryObject<Item> COBALT_INGOT = simple("cobalt_ingot", false);
    public static final RegistryObject<Item> RAW_TITANIUM = simple("raw_titanium", false);
    public static final RegistryObject<Item> TITANIUM_INGOT = simple("titanium_ingot", false);
    public static final RegistryObject<Item> RAW_INFERNIUM = simple("raw_infernium", true);
    public static final RegistryObject<Item> INFERNIUM_INGOT = simple("infernium_ingot", true);
    public static final RegistryObject<Item> RAW_AETHERIUM = simple("raw_aetherium", true);
    public static final RegistryObject<Item> AETHERIUM_INGOT = simple("aetherium_ingot", true);

    // ---- Dusts: made in Tiered Industries' Pulverizer, Ore Purifier and Chemical Washer ----
    public static final RegistryObject<Item> COBALT_DUST = simple("cobalt_dust", false);
    public static final RegistryObject<Item> TITANIUM_DUST = simple("titanium_dust", false);
    public static final RegistryObject<Item> INFERNIUM_DUST = simple("infernium_dust", true);
    public static final RegistryObject<Item> AETHERIUM_DUST = simple("aetherium_dust", true);

    // ---- Gear modules, fitted at the Armoury Workbench ----
    public static final RegistryObject<Item> BLANK_MODULE = simple("blank_module", false);
    public static final Map<GearModule, RegistryObject<Item>> MODULES = new EnumMap<>(GearModule.class);

    static {
        for (GearModule module : GearModule.values()) {
            MODULES.put(module, ITEMS.register(module.id() + "_module",
                    () -> new ModuleItem(module, new Item.Properties())));
        }
    }

    // ---- Upgrade templates for the smithing chain: Iron -> Cobalt -> Titanium -> Infernium -> Aetherium ----
    public static final RegistryObject<Item> COBALT_UPGRADE_TEMPLATE = template("cobalt");
    public static final RegistryObject<Item> TITANIUM_UPGRADE_TEMPLATE = template("titanium");
    public static final RegistryObject<Item> INFERNIUM_UPGRADE_TEMPLATE = template("infernium");
    public static final RegistryObject<Item> AETHERIUM_UPGRADE_TEMPLATE = template("aetherium");

    // ---- Weapons and armour, keyed by material id ("copper", "cobalt", ...) ----
    public static final Map<String, RegistryObject<Item>> SWORDS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> AXES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> HELMETS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> CHESTPLATES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> LEGGINGS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> BOOTS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> PICKAXES = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> SHOVELS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> HOES = new LinkedHashMap<>();

    // ---- Phase 4 weapon types (also come in Iron, so the upgrade chain has a start) ----
    public static final Map<String, RegistryObject<Item>> GREATSWORDS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> DAGGERS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> SPEARS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> WARHAMMERS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<Item>> BOWS = new LinkedHashMap<>();

    static {
        // Axe damage/speed follow vanilla's pattern: heavier hit, slower swing than the sword.
        gearSet(ModArmorMaterials.COPPER, ModToolTiers.COPPER, 6.5F, -3.15F);
        gearSet(ModArmorMaterials.EMERALD, ModToolTiers.EMERALD, 6.0F, -3.1F);
        gearSet(ModArmorMaterials.COBALT, ModToolTiers.COBALT, 6.0F, -3.05F);
        gearSet(ModArmorMaterials.TITANIUM, ModToolTiers.TITANIUM, 5.0F, -3.0F);
        gearSet(ModArmorMaterials.INFERNIUM, ModToolTiers.INFERNIUM, 5.0F, -3.0F);
        gearSet(ModArmorMaterials.AETHERIUM, ModToolTiers.AETHERIUM, 5.0F, -2.9F);

        // Weapon types: material, tier, lava-proof, bow durability, bow arrow damage bonus
        // (vanilla bow: 384 durability, arrow base damage 2)
        weaponSet("copper", ModToolTiers.COPPER, false, 300, 0.0F);
        weaponSet("emerald", ModToolTiers.EMERALD, false, 450, 0.5F);
        weaponSet("iron", Tiers.IRON, false, 465, 0.5F);
        weaponSet("cobalt", ModToolTiers.COBALT, false, 600, 1.0F);
        weaponSet("titanium", ModToolTiers.TITANIUM, false, 900, 1.5F);
        weaponSet("infernium", ModToolTiers.INFERNIUM, true, 1100, 2.0F);
        weaponSet("aetherium", ModToolTiers.AETHERIUM, true, 1500, 3.0F);
    }

    // ---- Shields (vanilla shield: 336 durability). Copper and Emerald are crafted; Cobalt and up
    //      are upgraded from the vanilla shield in the smithing table. ----
    public static final Map<String, RegistryObject<Item>> SHIELDS = new LinkedHashMap<>();

    static {
        shield("copper", 250, false, () -> Ingredient.of(Items.COPPER_INGOT));
        shield("emerald", 420, false, () -> Ingredient.of(Tags.Items.GEMS_EMERALD));
        shield("cobalt", 600, false, () -> Ingredient.of(ModToolTiers.INGOTS_COBALT));
        shield("titanium", 900, false, () -> Ingredient.of(ModToolTiers.INGOTS_TITANIUM));
        shield("infernium", 1200, true, () -> Ingredient.of(ModToolTiers.INGOTS_INFERNIUM));
        shield("aetherium", 1800, true, () -> Ingredient.of(ModToolTiers.INGOTS_AETHERIUM));
    }

    // ---- Crossbows (vanilla: 465 durability). Copper and Emerald are crafted; Cobalt and up
    //      are upgraded from the vanilla crossbow in the smithing table. ----
    public static final Map<String, RegistryObject<Item>> CROSSBOWS = new LinkedHashMap<>();

    static {
        crossbow("copper", 350, 0.0F, false, () -> Ingredient.of(Items.COPPER_INGOT));
        crossbow("emerald", 465, 0.5F, false, () -> Ingredient.of(Tags.Items.GEMS_EMERALD));
        crossbow("cobalt", 650, 1.0F, false, () -> Ingredient.of(ModToolTiers.INGOTS_COBALT));
        crossbow("titanium", 950, 1.5F, false, () -> Ingredient.of(ModToolTiers.INGOTS_TITANIUM));
        crossbow("infernium", 1150, 2.0F, true, () -> Ingredient.of(ModToolTiers.INGOTS_INFERNIUM));
        crossbow("aetherium", 1600, 3.0F, true, () -> Ingredient.of(ModToolTiers.INGOTS_AETHERIUM));
    }

    private static void crossbow(String id, int durability, float bonus, boolean fire, Supplier<Ingredient> repair) {
        CROSSBOWS.put(id, ITEMS.register(id + "_crossbow",
                () -> new TieredCrossbowItem(id, bonus, repair, props(fire).durability(durability))));
    }

    // ---- Quiver and tool belt ----
    public static final RegistryObject<Item> QUIVER = ITEMS.register("quiver",
            () -> new QuiverItem(new Item.Properties()));
    public static final RegistryObject<Item> TOOL_BELT = ITEMS.register("tool_belt",
            () -> new ToolBeltItem(new Item.Properties()));

    // ---- Aetherium Guardian boss ----
    public static final RegistryObject<Item> VOID_SIGIL = ITEMS.register("void_sigil",
            () -> new VoidSigilItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> GUARDIAN_CORE = ITEMS.register("guardian_core",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> GUARDIAN_SPAWN_EGG = ITEMS.register("aetherium_guardian_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.AETHERIUM_GUARDIAN, 0x241638, 0x9F60EC, new Item.Properties()));

    // ---- Creative testing tool (no recipe) ----
    public static final RegistryObject<Item> ANNIHILATOR = ITEMS.register("annihilator",
            () -> new AnnihilatorItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.EPIC)));

    // ---- Special arrows ----
    public static final RegistryObject<Item> EXPLOSIVE_ARROW = ITEMS.register("explosive_arrow",
            () -> new TieredArrowItem(TieredArrowItem.Kind.EXPLOSIVE, new Item.Properties()));
    public static final RegistryObject<Item> FIRE_ARROW = ITEMS.register("fire_arrow",
            () -> new TieredArrowItem(TieredArrowItem.Kind.FIRE, new Item.Properties()));
    public static final RegistryObject<Item> HEAVY_ARROW = ITEMS.register("heavy_arrow",
            () -> new TieredArrowItem(TieredArrowItem.Kind.HEAVY, new Item.Properties()));

    private static void shield(String id, int durability, boolean fire, Supplier<Ingredient> repair) {
        SHIELDS.put(id, ITEMS.register(id + "_shield",
                () -> new TieredShieldItem(id, repair, props(fire).durability(durability))));
    }

    private static void weaponSet(String id, Tier tier, boolean fire, int bowDurability, float arrowBonus) {
        // Greatsword: 1 + 5 + tier bonus damage, attack speed 1.1
        GREATSWORDS.put(id, ITEMS.register(id + "_greatsword",
                () -> new SwordItem(tier, 5, -2.9F, props(fire))));
        DAGGERS.put(id, ITEMS.register(id + "_dagger",
                () -> new DaggerItem(tier, props(fire))));
        SPEARS.put(id, ITEMS.register(id + "_spear",
                () -> new SpearItem(tier, props(fire))));
        WARHAMMERS.put(id, ITEMS.register(id + "_warhammer",
                () -> new WarHammerItem(tier, props(fire))));
        BOWS.put(id, ITEMS.register(id + "_bow",
                () -> new TieredBowItem(tier, arrowBonus, props(fire).durability(bowDurability))));
    }

    private static void gearSet(ModArmorMaterials armor, Tier tier, float axeDamage, float axeSpeed) {
        String id = armor.id();
        boolean fire = armor.isFireResistant();

        SWORDS.put(id, ITEMS.register(id + "_sword",
                () -> new SwordItem(tier, 3, -2.4F, props(fire))));
        AXES.put(id, ITEMS.register(id + "_axe",
                () -> new AxeItem(tier, axeDamage, axeSpeed, props(fire))));
        // Tools, with vanilla's damage and speed for each type
        PICKAXES.put(id, ITEMS.register(id + "_pickaxe",
                () -> new PickaxeItem(tier, 1, -2.8F, props(fire))));
        SHOVELS.put(id, ITEMS.register(id + "_shovel",
                () -> new ShovelItem(tier, 1.5F, -3.0F, props(fire))));
        HOES.put(id, ITEMS.register(id + "_hoe",
                () -> new HoeItem(tier, -(int) tier.getAttackDamageBonus(), 0.0F, props(fire))));

        HELMETS.put(id, ITEMS.register(id + "_helmet",
                () -> new TieredArmorItem(armor, ArmorItem.Type.HELMET, props(fire))));
        CHESTPLATES.put(id, ITEMS.register(id + "_chestplate",
                () -> new TieredArmorItem(armor, ArmorItem.Type.CHESTPLATE, props(fire))));
        LEGGINGS.put(id, ITEMS.register(id + "_leggings",
                () -> new TieredArmorItem(armor, ArmorItem.Type.LEGGINGS, props(fire))));
        BOOTS.put(id, ITEMS.register(id + "_boots",
                () -> new TieredArmorItem(armor, ArmorItem.Type.BOOTS, props(fire))));
    }

    private static RegistryObject<Item> template(String material) {
        return ITEMS.register(material + "_upgrade_smithing_template", () -> UpgradeTemplates.create(material));
    }

    private static RegistryObject<Item> simple(String name, boolean fireResistant) {
        return ITEMS.register(name, () -> new Item(props(fireResistant)));
    }

    private static Item.Properties props(boolean fireResistant) {
        Item.Properties props = new Item.Properties();
        if (fireResistant) {
            props.fireResistant();
        }
        return props;
    }
}
