package com.robvanblerk.tieredpower.radiation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Right-click to take: removes 300 rad of dose. */
public class IodineTabletsItem extends Item {
	public static final float REMOVES = 300;

	public IodineTabletsItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			Radiation.setDose(player, Radiation.dose(player) - REMOVES);
			player.playNotifySound(SoundEvents.GENERIC_EAT, net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
			player.displayClientMessage(Component.literal(String.format("Dose now %.0f rad", Radiation.dose(player))).withStyle(ChatFormatting.GREEN), true);
			if (!player.getAbilities().instabuild) stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click: removes 300 rad of radiation dose").withStyle(ChatFormatting.GRAY));
	}
}
