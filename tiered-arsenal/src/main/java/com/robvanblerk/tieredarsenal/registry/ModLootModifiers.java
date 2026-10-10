package com.robvanblerk.tieredarsenal.registry;

import com.mojang.serialization.Codec;
import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.loot.AddItemModifier;
import com.robvanblerk.tieredarsenal.loot.AutoSmeltModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    private ModLootModifiers() {}

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, TieredArsenal.MOD_ID);

    public static final RegistryObject<Codec<AddItemModifier>> ADD_ITEM =
            LOOT_MODIFIERS.register("add_item", AddItemModifier.CODEC);
    public static final RegistryObject<Codec<AutoSmeltModifier>> AUTO_SMELT =
            LOOT_MODIFIERS.register("auto_smelt", AutoSmeltModifier.CODEC);
}
