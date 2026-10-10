package com.robvanblerk.tieredpower.planet;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Void Pearl, from the Void Reach: an ender pearl that comes back - throw it as often as you like (3 second cooldown). */
public class VoidPearlItem extends Item {
	public static final int COOLDOWN = 60;

	public VoidPearlItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_PEARL_THROW, SoundSource.NEUTRAL, 0.5f, 0.6f);
		player.getCooldowns().addCooldown(this, COOLDOWN);
		if (!level.isClientSide()) {
			ThrownEnderpearl pearl = new ThrownEnderpearl(level, player);
			pearl.setItem(new ItemStack(net.minecraft.world.item.Items.ENDER_PEARL));
			pearl.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.6f, 1f);
			level.addFreshEntity(pearl);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
