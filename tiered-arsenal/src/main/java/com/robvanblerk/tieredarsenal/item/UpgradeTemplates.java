package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

/**
 * Upgrade templates for the tier chain, used in the vanilla smithing table:
 *   Iron -> Cobalt -> Titanium -> Infernium -> Aetherium
 * Works like the netherite upgrade: previous-tier piece + template + ingot,
 * keeping enchantments, custom name and armour trim.
 */
public final class UpgradeTemplates {
    private UpgradeTemplates() {}

    // Ghost icons shown in the empty smithing slots (vanilla textures)
    private static final List<ResourceLocation> BASE_SLOT_ICONS = List.of(
            new ResourceLocation("item/empty_armor_slot_helmet"),
            new ResourceLocation("item/empty_slot_sword"),
            new ResourceLocation("item/empty_armor_slot_chestplate"),
            new ResourceLocation("item/empty_slot_axe"),
            new ResourceLocation("item/empty_armor_slot_leggings"),
            new ResourceLocation("item/empty_armor_slot_boots"));

    private static final List<ResourceLocation> ADDITION_SLOT_ICONS = List.of(
            new ResourceLocation("item/empty_slot_ingot"));

    public static SmithingTemplateItem create(String material) {
        String key = "item." + TieredArsenal.MOD_ID + ".smithing_template." + material + "_upgrade";
        return new SmithingTemplateItem(
                Component.translatable(key + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("upgrade." + TieredArsenal.MOD_ID + "." + material + "_upgrade")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(key + ".base_slot_description"),
                Component.translatable(key + ".additions_slot_description"),
                BASE_SLOT_ICONS,
                ADDITION_SLOT_ICONS);
    }
}
