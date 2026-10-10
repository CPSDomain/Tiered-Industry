package com.robvanblerk.tieredpower.planet;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Abyssal Pearl (Abyss planet): crush it in your hand for 10 minutes of water breathing, night vision and dolphin's grace. */
public class AbyssalPearlItem extends Item {
	public static final int TICKS = 12_000;

	public AbyssalPearlItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, TICKS, 0, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, TICKS, 0, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, TICKS, 0, false, true));
			level.playSound(null, player.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.3f);
			if (!player.getAbilities().instabuild) stack.shrink(1);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Right-click: 10 minutes of water breathing, night vision and dolphin's grace").withStyle(ChatFormatting.AQUA));
	}
}
