package com.robvanblerk.tieredpower.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.robvanblerk.tieredpower.item.powered.ItemEnergy;
import com.robvanblerk.tieredpower.item.powered.JetpackItem;
import com.robvanblerk.tieredpower.network.ModNetwork;

/** Client-side jetpack flight: reads the jump key, pushes the player up, and tells the server so it can charge energy. */
public final class JetpackClient {
	private static boolean lastSent;

	public static void tick(Player player, ItemStack stack, JetpackItem jetpack) {
		Minecraft mc = Minecraft.getInstance();
		if (player != mc.player) return;
		boolean flying = mc.screen == null && mc.options.keyJump.isDown() && jetpack.fuel(stack) >= jetpack.getCostPerTick();
		if (flying) {
			Vec3 v = player.getDeltaMovement();
			player.setDeltaMovement(v.x, Math.min(v.y + jetpack.getThrust(), jetpack.getMaxRise()), v.z);
			player.fallDistance = 0;
			// little puffs of smoke from the thrusters
			if (player.tickCount % 2 == 0) {
				Vec3 back = player.getLookAngle().multiply(-0.4, 0, -0.4);
				player.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
						player.getX() + back.x, player.getY() + 0.8, player.getZ() + back.z, 0, -0.15, 0);
			}
		}
		if (flying != lastSent) {
			lastSent = flying;
			ModNetwork.CHANNEL.sendToServer(new ModNetwork.JetpackPacket(flying));
		}
	}

	private JetpackClient() {}
}
