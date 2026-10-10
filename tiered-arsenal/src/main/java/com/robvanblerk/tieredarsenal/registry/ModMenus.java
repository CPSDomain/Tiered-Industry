package com.robvanblerk.tieredarsenal.registry;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.menu.ArmouryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, TieredArsenal.MOD_ID);

    public static final RegistryObject<MenuType<ArmouryMenu>> ARMOURY_WORKBENCH =
            MENUS.register("armoury_workbench", () -> IForgeMenuType.create(ArmouryMenu::new));
}
