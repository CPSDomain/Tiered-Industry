package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.item.WirelessTerminalItem;

/** Client -> server: the Open Wireless Terminal key was pressed. */
public record OpenWirelessPacket() {
	public void encode(FriendlyByteBuf buf) {}

	public static OpenWirelessPacket decode(FriendlyByteBuf buf) {
		return new OpenWirelessPacket();
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player == null) return;
			int slot = WirelessTerminalItem.findSlot(player);
			if (slot < 0) player.displayClientMessage(Component.literal("No Wireless Terminal in your inventory").withStyle(ChatFormatting.RED), true);
			else WirelessTerminalItem.open(player, slot);
		});
		ctx.get().setPacketHandled(true);
	}
}
