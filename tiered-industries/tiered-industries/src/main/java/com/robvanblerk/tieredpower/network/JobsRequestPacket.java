package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;

/** Client -> server: send me the crafting job list (cancel = -1), or cancel job number 'cancel' first. */
public record JobsRequestPacket(int containerId, int cancel) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeVarInt(cancel + 1);
	}

	public static JobsRequestPacket decode(FriendlyByteBuf buf) {
		return new JobsRequestPacket(buf.readVarInt(), buf.readVarInt() - 1);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof StorageTerminalMenu menu && menu.containerId == containerId) menu.handleJobsRequest(cancel);
		});
		ctx.get().setPacketHandled(true);
	}
}
