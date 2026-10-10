package com.robvanblerk.tieredarsenal;

import com.robvanblerk.tieredarsenal.config.ArsenalConfig;
import com.robvanblerk.tieredarsenal.material.ModToolTiers;
import com.robvanblerk.tieredarsenal.network.ModNetwork;
import com.robvanblerk.tieredarsenal.registry.ModBlockEntities;
import com.robvanblerk.tieredarsenal.registry.ModBlocks;
import com.robvanblerk.tieredarsenal.registry.ModEnchantments;
import com.robvanblerk.tieredarsenal.registry.ModEntities;
import com.robvanblerk.tieredarsenal.registry.ModLootModifiers;
import com.robvanblerk.tieredarsenal.registry.ModMenus;
import com.robvanblerk.tieredarsenal.registry.ModCreativeTabs;
import com.robvanblerk.tieredarsenal.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Tiered Arsenal - weapons and armour companion to Tiered Industries.
 *
 * Progression (weakest to strongest):
 *   Copper -> Emerald -> Cobalt -> Titanium -> Infernium -> Aetherium
 * Copper and Emerald use vanilla materials; the other four come from new ores.
 */
@Mod(TieredArsenal.MOD_ID)
public class TieredArsenal {
    public static final String MOD_ID = "tieredarsenal";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TieredArsenal() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Tiers must be registered with Forge's tier sorting before any items use them.
        ModToolTiers.init();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ArsenalConfig.SPEC);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);
        ModEnchantments.ENCHANTMENTS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModNetwork.register();

        LOGGER.info("Tiered Arsenal loaded");
    }
}
