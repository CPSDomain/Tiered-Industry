package com.robvanblerk.tieredarsenal.module;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.item.TieredBowItem;
import com.robvanblerk.tieredarsenal.item.TieredCrossbowItem;
import com.robvanblerk.tieredarsenal.material.ModArmorMaterials;
import com.robvanblerk.tieredarsenal.material.ModToolTiers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads and writes the modules stored on a piece of gear. */
public final class ModuleHelper {
    private ModuleHelper() {}

    public static final String NBT_KEY = "TieredModules";

    /** Copper, Emerald and Iron 1 slot; Cobalt 2; Titanium 3; Infernium 4; Aetherium 5. Shields have none. */
    private static final Map<String, Integer> SLOTS_BY_MATERIAL = Map.of(
            "copper", 1, "emerald", 1, "iron", 1,
            "cobalt", 2, "titanium", 3, "infernium", 4, "aetherium", 5);

    /** Number of module slots on this stack. 0 for anything that isn't Tiered Arsenal gear. */
    public static int getSlots(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !TieredArsenal.MOD_ID.equals(id.getNamespace())) return 0;

        if (stack.getItem() instanceof ArmorItem armour && armour.getMaterial() instanceof ModArmorMaterials mat) {
            return SLOTS_BY_MATERIAL.getOrDefault(mat.id(), 0);
        }
        if (stack.getItem() instanceof TieredCrossbowItem crossbow) {
            return SLOTS_BY_MATERIAL.getOrDefault(crossbow.getMaterial(), 0);
        }
        Tier tier = null;
        if (stack.getItem() instanceof TieredItem tiered) tier = tiered.getTier();
        if (stack.getItem() instanceof TieredBowItem bow) tier = bow.getTier();
        if (tier == null) return 0;
        if (tier == ModToolTiers.COPPER || tier == ModToolTiers.EMERALD || tier == Tiers.IRON) return 1;
        if (tier == ModToolTiers.COBALT) return 2;
        if (tier == ModToolTiers.TITANIUM) return 3;
        if (tier == ModToolTiers.INFERNIUM) return 4;
        if (tier == ModToolTiers.AETHERIUM) return 5;
        return 0;
    }

    public static List<GearModule> getModules(ItemStack stack) {
        List<GearModule> list = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(NBT_KEY, Tag.TAG_LIST)) return list;
        for (Tag t : tag.getList(NBT_KEY, Tag.TAG_STRING)) {
            GearModule m = GearModule.byId(t.getAsString());
            if (m != null) list.add(m);
        }
        return list;
    }

    public static boolean has(ItemStack stack, GearModule module) {
        return getModules(stack).contains(module);
    }

    public static void add(ItemStack stack, GearModule module) {
        ListTag list = stack.getOrCreateTag().getList(NBT_KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(module.id()));
        stack.getOrCreateTag().put(NBT_KEY, list);
    }

    /** Removes the module at this position in the list and returns it, or null. */
    public static GearModule removeAt(ItemStack stack, int index) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        ListTag list = tag.getList(NBT_KEY, Tag.TAG_STRING);
        if (index < 0 || index >= list.size()) return null;
        GearModule removed = GearModule.byId(list.getString(index));
        list.remove(index);
        if (list.isEmpty()) {
            tag.remove(NBT_KEY);
        } else {
            tag.put(NBT_KEY, list);
        }
        return removed;
    }
}
