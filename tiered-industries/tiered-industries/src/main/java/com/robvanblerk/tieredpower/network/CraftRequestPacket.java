package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;

/** Client -> server: plan (start = false) or start (start = true) crafting an item from a terminal. */
public record CraftRequestPacket(int containerId, ItemStack item, long amount, boolean start) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeItem(item);
		buf.writeVarLong(amount);
		buf.writeBoolean(start);
	}

	public static CraftRequestPacket decode(FriendlyByteBuf buf) {
		return new CraftRequestPacket(buf.readVarInt(), buf.readItem(), buf.readVarLong(), buf.readBoolean());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof StorageTerminalMenu menu && menu.containerId == containerId)
				menu.handleCraftRequest(item, amount, start);
		});
		ctx.get().setPacketHandled(true);
	}
}
