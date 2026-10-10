package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.logic.LogicControllerMenu;

/** Client -> server: a Logic Controller rule's number. */
public record LogicValuePacket(int containerId, int rule, int value) {
	public void encode(FriendlyByteBuf buf) { buf.writeVarInt(containerId); buf.writeVarInt(rule); buf.writeVarInt(value); }
	public static LogicValuePacket decode(FriendlyByteBuf buf) { return new LogicValuePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt()); }

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var p = ctx.get().getSender();
			if (p != null && p.containerMenu instanceof LogicControllerMenu m && m.containerId == containerId) m.setThreshold(rule, value);
		});
		ctx.get().setPacketHandled(true);
	}
}
