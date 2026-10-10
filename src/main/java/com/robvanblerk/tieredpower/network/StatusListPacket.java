package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.logic.MachineStatusMenu;

/** Server -> client: the machine list for an open Machine Status Display. */
public record StatusListPacket(int containerId, List<String> lines) {
	private static final int MAX = 300;

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		int n = Math.min(lines.size(), MAX);
		buf.writeVarInt(n);
		for (int i = 0; i < n; i++) buf.writeUtf(lines.get(i), 200);
	}

	public static StatusListPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt(), n = buf.readVarInt();
		List<String> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(buf.readUtf(200));
		return new StatusListPacket(id, list);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var p = Minecraft.getInstance().player;
			if (p != null && p.containerMenu instanceof MachineStatusMenu menu && menu.containerId == containerId) menu.setLines(lines);
		}));
		ctx.get().setPacketHandled(true);
	}
}
