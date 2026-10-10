package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.StockKeeperMenu;

/** Client -> server: set how many of a Stock Keeper slot's item to keep in storage. */
public record StockKeeperPacket(int containerId, int slot, int amount) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeVarInt(slot);
		buf.writeVarInt(amount);
	}

	public static StockKeeperPacket decode(FriendlyByteBuf buf) {
		return new StockKeeperPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof StockKeeperMenu menu && menu.containerId == containerId) menu.setAmount(slot, amount);
		});
		ctx.get().setPacketHandled(true);
	}
}
