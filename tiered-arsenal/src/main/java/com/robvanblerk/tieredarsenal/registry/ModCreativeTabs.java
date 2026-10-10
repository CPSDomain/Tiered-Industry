package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TieredArsenal.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ARSENAL = TABS.register("arsenal", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + TieredArsenal.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.SWORDS.get("aetherium").get()))
                    .displayItems((params, output) ->
                            ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
                    .build());
}
