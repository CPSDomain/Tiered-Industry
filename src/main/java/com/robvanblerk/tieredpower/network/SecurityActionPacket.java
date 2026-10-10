package com.robvanblerk.tieredpower.network;

import java.util.UUID;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.SecurityTerminalMenu;

/** Client -> server: Security Terminal action - 0 list, 1 add a player by name, 2 remove a player. */
public record SecurityActionPacket(int containerId, int action, String name, @Nullable UUID id) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeVarInt(action);
		buf.writeUtf(name, 64);
		buf.writeBoolean(id != null);
		if (id != null) buf.writeUUID(id);
	}

	public static SecurityActionPacket decode(FriendlyByteBuf buf) {
		int cid = buf.readVarInt(), action = buf.readVarInt();
		String name = buf.readUtf(64);
		UUID id = buf.readBoolean() ? buf.readUUID() : null;
		return new SecurityActionPacket(cid, action, name, id);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof SecurityTerminalMenu menu && menu.containerId == containerId) menu.handle(action, name, id);
		});
		ctx.get().setPacketHandled(true);
	}
}
