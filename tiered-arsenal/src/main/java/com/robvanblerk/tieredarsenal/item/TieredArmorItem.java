package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.material.ModArmorMaterials;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armour piece that shows its full-set bonus in the tooltip. */
public class TieredArmorItem extends ArmorItem {
    private final ModArmorMaterials tieredMaterial;

    public TieredArmorItem(ModArmorMaterials material, Type type, Properties properties) {
        super(material, type, properties);
        this.tieredMaterial = material;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (tieredMaterial.hasSetBonus()) {
            tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".set_bonus")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".set_bonus." + tieredMaterial.id())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
