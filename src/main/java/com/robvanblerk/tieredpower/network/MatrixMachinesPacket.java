package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.MatrixControllerMenu;

/** Server -> client: the machines an Assembly Matrix can use, for its screen. */
public record MatrixMachinesPacket(int containerId, List<String> machines) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeVarInt(Math.min(machines.size(), 40));
		for (int i = 0; i < machines.size() && i < 40; i++) buf.writeUtf(machines.get(i), 120);
	}

	public static MatrixMachinesPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt(), n = buf.readVarInt();
		List<String> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(buf.readUtf(120));
		return new MatrixMachinesPacket(id, list);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var p = Minecraft.getInstance().player;
			if (p != null && p.containerMenu instanceof MatrixControllerMenu menu && menu.containerId == containerId) menu.setMachines(machines);
		}));
		ctx.get().setPacketHandled(true);
	}
}
