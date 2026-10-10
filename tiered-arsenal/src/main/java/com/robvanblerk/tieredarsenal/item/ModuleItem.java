package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.module.GearModule;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** A module ready to fit at the Armoury Workbench. */
public class ModuleItem extends Item {
    private final GearModule module;

    public ModuleItem(GearModule module, Properties properties) {
        super(properties.stacksTo(16));
        this.module = module;
    }

    public GearModule getModule() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String base = "tooltip." + TieredArsenal.MOD_ID + ".module.";
        tooltip.add(Component.translatable(base + module.id()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(base + "fits." + module.fits().name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
    }
}
