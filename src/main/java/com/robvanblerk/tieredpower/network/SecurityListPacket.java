package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.SecurityTerminalMenu;

/** Server -> client: the Security Terminal's owner and trusted players. */
public record SecurityListPacket(int containerId, String owner, List<Member> members) {
	public record Member(UUID id, String name) {}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeUtf(owner, 64);
		buf.writeVarInt(members.size());
		for (Member m : members) {
			buf.writeUUID(m.id());
			buf.writeUtf(m.name(), 64);
		}
	}

	public static SecurityListPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		String owner = buf.readUtf(64);
		int n = Math.min(buf.readVarInt(), 200);
		List<Member> members = new ArrayList<>(n);
		for (int i = 0; i < n; i++) members.add(new Member(buf.readUUID(), buf.readUtf(64)));
		return new SecurityListPacket(id, owner, members);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var p = Minecraft.getInstance().player;
			if (p != null && p.containerMenu instanceof SecurityTerminalMenu menu && menu.containerId == containerId) menu.setClientData(owner, members);
		}));
		ctx.get().setPacketHandled(true);
	}
}
