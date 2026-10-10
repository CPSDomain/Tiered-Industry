package com.robvanblerk.tieredpower.radiation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Hold it to see the radiation around you (after shielding) and your dose; it clicks faster the hotter it gets. */
public class GeigerCounterItem extends Item {
	public GeigerCounterItem(Properties properties) {
		super(properties);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (level.isClientSide() || !(entity instanceof ServerPlayer p)) return;
		boolean held = selected || p.getOffhandItem() == stack;
		if (!held || p.tickCount % 10 != 0) return;
		if (!Radiation.enabled()) {
			if (p.tickCount % 40 == 0) p.displayClientMessage(Component.literal("Radiation is switched off in the config").withStyle(ChatFormatting.GRAY), true);
			return;
		}
		float rate = Radiation.exposure(p) * (1 - Radiation.protection(p));
		float dose = Radiation.dose(p);
		ChatFormatting colour = rate <= 0.05f ? ChatFormatting.GREEN : rate < 5 ? ChatFormatting.YELLOW : ChatFormatting.RED;
		p.displayClientMessage(Component.literal(String.format("Radiation %.1f rad/s  |  Dose %.0f rad%s", rate, dose,
				dose >= 1000 ? " - DANGER" : dose >= 300 ? " - high" : "")).withStyle(colour), true);
		if (rate > 0.05f && level.random.nextFloat() < Math.min(1f, rate / 10f))
			level.playSound(null, p.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.3f, 2.0f);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Hold it to measure radiation and your dose").withStyle(ChatFormatting.GRAY));
	}
}
