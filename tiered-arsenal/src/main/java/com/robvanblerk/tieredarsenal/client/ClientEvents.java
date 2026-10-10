package com.robvanblerk.tieredarsenal.client;

import com.robvanblerk.tieredarsenal.TieredArsenal;
import com.robvanblerk.tieredarsenal.module.GearModule;
import com.robvanblerk.tieredarsenal.module.ModuleHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Shows fitted modules and free slots on every piece of Tiered Arsenal gear. */
@Mod.EventBusSubscriber(modid = TieredArsenal.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        int slots = ModuleHelper.getSlots(event.getItemStack());
        if (slots <= 0) return;
        List<GearModule> fitted = ModuleHelper.getModules(event.getItemStack());
        List<Component> tip = event.getToolTip();
        int at = Math.min(1, tip.size()); // just under the item name
        tip.add(at++, Component.translatable("tooltip." + TieredArsenal.MOD_ID + ".modules", fitted.size(), slots)
                .withStyle(ChatFormatting.GRAY));
        for (GearModule m : fitted) {
            tip.add(at++, Component.literal("  ")
                    .append(Component.translatable("module." + TieredArsenal.MOD_ID + "." + m.id()))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
