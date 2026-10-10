package com.robvanblerk.tieredpower.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;

/** Every Tiered Industries item: "Hold Shift for details", and holding Shift shows its guide description. */
@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TooltipEvents {
	private static final int WIDTH = 42;

	@SubscribeEvent
	public static void onTooltip(ItemTooltipEvent event) {
		ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
		if (!TieredPower.MOD_ID.equals(id.getNamespace())) return;
		String key = "tooltip.tieredpower.desc." + id.getPath();
		if (!I18n.exists(key)) return;
		if (!Screen.hasShiftDown()) {
			event.getToolTip().add(Component.translatable("tooltip.tieredpower.hold_shift").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
			return;
		}
		for (String line : wrap(I18n.get(key))) event.getToolTip().add(Component.literal(line).withStyle(ChatFormatting.GRAY));
	}

	private static List<String> wrap(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (line.length() > 0 && line.length() + word.length() + 1 > WIDTH) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (line.length() > 0) line.append(' ');
			line.append(word);
		}
		if (line.length() > 0) lines.add(line.toString());
		return lines;
	}

	private TooltipEvents() {}
}
