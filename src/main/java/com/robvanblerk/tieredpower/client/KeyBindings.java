package com.robvanblerk.tieredpower.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.robvanblerk.tieredpower.TieredPower;
import com.robvanblerk.tieredpower.network.ModNetwork;
import com.robvanblerk.tieredpower.network.OpenWirelessPacket;

/** The "Open Wireless Terminal" key (unbound by default - set it in Controls > Key Binds > Tiered Industries). */
public final class KeyBindings {
	public static final KeyMapping OPEN_WIRELESS = new KeyMapping("key.tieredpower.open_wireless", KeyConflictContext.IN_GAME,
			InputConstants.UNKNOWN, "key.categories.tieredpower");
	/** Quantum Suit modules (default V). */
	public static final KeyMapping SUIT_MODULES = new KeyMapping("key.tieredpower.suit_modules", KeyConflictContext.IN_GAME,
			InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_V, "key.categories.tieredpower");

	@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class Register {
		@SubscribeEvent
		public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
			event.register(OPEN_WIRELESS);
			event.register(SUIT_MODULES);
		}
	}

	@Mod.EventBusSubscriber(modid = TieredPower.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
	public static final class Ticks {
		@SubscribeEvent
		public static void onClientTick(TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) return;
			Minecraft mc = Minecraft.getInstance();
			while (OPEN_WIRELESS.consumeClick()) {
				if (mc.player != null && mc.screen == null) ModNetwork.CHANNEL.sendToServer(new OpenWirelessPacket());
			}
			while (SUIT_MODULES.consumeClick()) {
				if (mc.player != null && mc.screen == null) mc.setScreen(new com.robvanblerk.tieredpower.client.screen.SuitModulesScreen());
			}
			// Elevators: jumping or sneaking while standing on one.
			if (mc.player != null && mc.screen == null) {
				boolean jump = mc.player.input.jumping, sneak = mc.player.input.shiftKeyDown;
				if (com.robvanblerk.tieredpower.elevator.ElevatorBlock.under(mc.player) != null) {
					if (jump && !wasJumping) ModNetwork.CHANNEL.sendToServer(new com.robvanblerk.tieredpower.network.ElevatorPacket(true));
					else if (sneak && !wasSneaking) ModNetwork.CHANNEL.sendToServer(new com.robvanblerk.tieredpower.network.ElevatorPacket(false));
				}
				wasJumping = jump;
				wasSneaking = sneak;
			}
		}

		private static boolean wasJumping, wasSneaking;
	}

	private KeyBindings() {}
}
