package com.robvanblerk.tieredarsenal.item;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fast, light blade. Hitting a mob from behind does 1.5x damage
 * (handled in event/WeaponEvents.java).
 */
public class DaggerItem extends SwordItem {
    public static final float BACKSTAB_MULTIPLIER = 1.5F;

    public DaggerItem(Tier tier, Properties properties) {
        // 1 + 1 + tier bonus damage, attack speed 2.2 (sword is 1.6): about a sword's damage per second,
        // more from behind
        super(tier, 1, -1.8F, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".dagger").withStyle(ChatFormatting.GOLD));
    }
}
