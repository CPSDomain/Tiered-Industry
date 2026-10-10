package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.elevator.ElevatorBlock;

/** Client -> server: the player standing on an Elevator jumped (up) or sneaked (down). */
public record ElevatorPacket(boolean up) {
	public void encode(FriendlyByteBuf buf) { buf.writeBoolean(up); }
	public static ElevatorPacket decode(FriendlyByteBuf buf) { return new ElevatorPacket(buf.readBoolean()); }

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var p = ctx.get().getSender();
			if (p != null && !p.isSpectator()) ElevatorBlock.travel(p, up);
		});
		ctx.get().setPacketHandled(true);
	}
}
