package com.robvanblerk.tieredarsenal.compat.jei;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.registry.ModBlocks;
import com.robvanblerk.tieredarsenal.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * JEI info pages, like Tiered Industries has. Only loaded when JEI is installed.
 * Text lives in the lang file under jei.tieredarsenal.info.<key>.
 */
@JeiPlugin
public class TieredArsenalJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(TieredArsenal.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration reg) {
        // Ores, raw ore and blocks
        info(reg, "cobalt_ore", ModBlocks.COBALT_ORE, ModBlocks.DEEPSLATE_COBALT_ORE, ModItems.RAW_COBALT);
        info(reg, "titanium_ore", ModBlocks.TITANIUM_ORE, ModBlocks.DEEPSLATE_TITANIUM_ORE, ModItems.RAW_TITANIUM);
        info(reg, "infernium_ore", ModBlocks.NETHER_INFERNIUM_ORE, ModItems.RAW_INFERNIUM);
        info(reg, "aetherium_ore", ModBlocks.END_AETHERIUM_ORE, ModItems.RAW_AETHERIUM);
        info(reg, "dust", ModItems.COBALT_DUST, ModItems.TITANIUM_DUST, ModItems.INFERNIUM_DUST, ModItems.AETHERIUM_DUST);
        info(reg, "gated_ingot", ModItems.TITANIUM_INGOT, ModItems.INFERNIUM_INGOT, ModItems.AETHERIUM_INGOT);

        // Upgrade chain
        info(reg, "upgrade_template", ModItems.COBALT_UPGRADE_TEMPLATE, ModItems.TITANIUM_UPGRADE_TEMPLATE,
                ModItems.INFERNIUM_UPGRADE_TEMPLATE, ModItems.AETHERIUM_UPGRADE_TEMPLATE);

        // Weapon types and shields (one page shared by every tier)
        infoAll(reg, "greatsword", ModItems.GREATSWORDS.values());
        infoAll(reg, "dagger", ModItems.DAGGERS.values());
        infoAll(reg, "spear", ModItems.SPEARS.values());
        infoAll(reg, "warhammer", ModItems.WARHAMMERS.values());
        infoAll(reg, "bow", ModItems.BOWS.values());
        infoAll(reg, "shield", ModItems.SHIELDS.values());
        infoAll(reg, "crossbow", ModItems.CROSSBOWS.values());
        List<RegistryObject<Item>> tools = new ArrayList<>();
        tools.addAll(ModItems.PICKAXES.values());
        tools.addAll(ModItems.SHOVELS.values());
        tools.addAll(ModItems.HOES.values());
        infoAll(reg, "tools", tools);
        info(reg, "explosive_arrow", ModItems.EXPLOSIVE_ARROW);
        info(reg, "fire_arrow", ModItems.FIRE_ARROW);
        info(reg, "heavy_arrow", ModItems.HEAVY_ARROW);
        info(reg, "quiver", ModItems.QUIVER);
        info(reg, "tool_belt", ModItems.TOOL_BELT);
        info(reg, "void_sigil", ModItems.VOID_SIGIL, ModItems.GUARDIAN_SPAWN_EGG);
        info(reg, "guardian_core", ModItems.GUARDIAN_CORE);
        info(reg, "annihilator", ModItems.ANNIHILATOR);

        // Modules and the workbench
        info(reg, "armoury_workbench", ModBlocks.ARMOURY_WORKBENCH);
        info(reg, "blank_module", ModItems.BLANK_MODULE);
        for (Map.Entry<GearModule, RegistryObject<Item>> e : ModItems.MODULES.entrySet()) {
            info(reg, "module." + e.getKey().id(), e.getValue());
        }
    }

    @SafeVarargs
    private static void info(IRecipeRegistration reg, String key, Supplier<? extends ItemLike>... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Supplier<? extends ItemLike> item : items) {
            stacks.add(new ItemStack(item.get()));
        }
        add(reg, key, stacks);
    }

    private static void infoAll(IRecipeRegistration reg, String key, Collection<RegistryObject<Item>> items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (RegistryObject<Item> item : items) {
            stacks.add(new ItemStack(item.get()));
        }
        add(reg, key, stacks);
    }

    private static void add(IRecipeRegistration reg, String key, List<ItemStack> stacks) {
        reg.addIngredientInfo(stacks, VanillaTypes.ITEM_STACK,
                Component.translatable("jei." + TieredArsenal.MOD_ID + ".info." + key));
    }
}
