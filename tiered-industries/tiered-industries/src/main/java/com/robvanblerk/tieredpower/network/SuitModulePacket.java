package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.item.powered.SuitModules;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Client -> server: switch a worn Quantum Suit module on/off (action 0) or take it out (action 1). */
public record SuitModulePacket(String module, int action) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeUtf(module, 32);
		buf.writeVarInt(action);
	}

	public static SuitModulePacket decode(FriendlyByteBuf buf) {
		return new SuitModulePacket(buf.readUtf(32), buf.readVarInt());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			var m = SuitModules.Module.byId(module);
			if (player == null || m == null) return;
			ItemStack armor = player.getItemBySlot(m.slot);
			if (!SuitModules.installed(armor, m)) return;
			if (action == 0) {
				boolean on = !SuitModules.switchedOn(armor, m);
				SuitModules.setOn(armor, m, on);
				player.displayClientMessage(Component.literal(m.title + (on ? " on" : " off")).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
			} else if (action == 1) {
				SuitModules.remove(armor, m);
				ItemStack item = new ItemStack(ModBlocks.SUIT_MODULES.get(m).get());
				if (!player.getInventory().add(item)) player.drop(item, false);
				player.displayClientMessage(Component.literal(m.title + " removed"), true);
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
