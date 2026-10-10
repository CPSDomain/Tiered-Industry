package com.robvanblerk.tieredarsenal.material;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

/**
 * Tool tiers used by the swords and axes.
 *
 * ForgeTier args: (mining level, durability, mining speed, bonus attack damage,
 *                  enchantability, "blocks that need this tier" tag, repair item)
 *
 * Vanilla for comparison:
 *   Iron      : level 2,  250 uses, speed 6,  +2 dmg, ench 14
 *   Diamond   : level 3, 1561 uses, speed 8,  +3 dmg, ench 10
 *   Netherite : level 4, 2031 uses, speed 9,  +4 dmg, ench 15
 */
public final class ModToolTiers {
    private ModToolTiers() {}

    public static final TagKey<Block> NEEDS_COPPER_TOOL = blockTag("needs_copper_tool");
    public static final TagKey<Block> NEEDS_EMERALD_TOOL = blockTag("needs_emerald_tool");
    public static final TagKey<Block> NEEDS_COBALT_TOOL = blockTag("needs_cobalt_tool");
    public static final TagKey<Block> NEEDS_TITANIUM_TOOL = blockTag("needs_titanium_tool");
    public static final TagKey<Block> NEEDS_INFERNIUM_TOOL = blockTag("needs_infernium_tool");
    public static final TagKey<Block> NEEDS_AETHERIUM_TOOL = blockTag("needs_aetherium_tool");

    public static final TagKey<Item> INGOTS_COBALT = forgeItemTag("ingots/cobalt");
    public static final TagKey<Item> INGOTS_TITANIUM = forgeItemTag("ingots/titanium");
    public static final TagKey<Item> INGOTS_INFERNIUM = forgeItemTag("ingots/infernium");
    public static final TagKey<Item> INGOTS_AETHERIUM = forgeItemTag("ingots/aetherium");

    public static final Tier COPPER = register("copper",
            new ForgeTier(1, 200, 5.0F, 1.5F, 13, NEEDS_COPPER_TOOL, () -> Ingredient.of(Items.COPPER_INGOT)),
            List.of(Tiers.STONE), List.of(Tiers.IRON));

    public static final Tier EMERALD = register("emerald",
            new ForgeTier(2, 900, 7.0F, 2.5F, 22, NEEDS_EMERALD_TOOL, () -> Ingredient.of(Tags.Items.GEMS_EMERALD)),
            List.of(Tiers.IRON), List.of(Tiers.DIAMOND));

    public static final Tier COBALT = register("cobalt",
            new ForgeTier(2, 1100, 7.5F, 2.5F, 12, NEEDS_COBALT_TOOL, () -> Ingredient.of(INGOTS_COBALT)),
            List.of(EMERALD), List.of(Tiers.DIAMOND));

    public static final Tier TITANIUM = register("titanium",
            new ForgeTier(3, 2000, 8.5F, 3.5F, 14, NEEDS_TITANIUM_TOOL, () -> Ingredient.of(INGOTS_TITANIUM)),
            List.of(Tiers.DIAMOND), List.of(Tiers.NETHERITE));

    public static final Tier INFERNIUM = register("infernium",
            new ForgeTier(4, 2400, 9.0F, 4.5F, 16, NEEDS_INFERNIUM_TOOL, () -> Ingredient.of(INGOTS_INFERNIUM)),
            List.of(Tiers.NETHERITE), List.of());

    public static final Tier AETHERIUM = register("aetherium",
            new ForgeTier(5, 3200, 10.0F, 5.5F, 20, NEEDS_AETHERIUM_TOOL, () -> Ingredient.of(INGOTS_AETHERIUM)),
            List.of(INFERNIUM), List.of());

    /** Called from the mod constructor so the static fields above are created early. */
    public static void init() {}

    private static Tier register(String name, Tier tier, List<Object> after, List<Object> before) {
        return TierSortingRegistry.registerTier(tier, new ResourceLocation(TieredArsenal.MOD_ID, name), after, before);
    }

    private static TagKey<Block> blockTag(String name) {
        return BlockTags.create(new ResourceLocation(TieredArsenal.MOD_ID, name));
    }

    private static TagKey<Item> forgeItemTag(String name) {
        return ItemTags.create(new ResourceLocation("forge", name));
    }
}
