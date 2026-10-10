package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Server -> client: the answer to a craft request. kind per line: 0 = taken from storage, 1 = crafted, 2 = missing.
 * started = the job was queued (the request screen closes).
 */
public record CraftPlanPacket(boolean started, String message, List<Line> lines) {
	public static final int USED = 0, CRAFTED = 1, MISSING = 2, MAX_LINES = 60;

	public record Line(ItemStack item, long count, int kind) {}

	public void encode(FriendlyByteBuf buf) {
		buf.writeBoolean(started);
		buf.writeUtf(message, 256);
		int n = Math.min(lines.size(), MAX_LINES);
		buf.writeVarInt(n);
		for (int i = 0; i < n; i++) {
			buf.writeItem(lines.get(i).item());
			buf.writeVarLong(lines.get(i).count());
			buf.writeByte(lines.get(i).kind());
		}
	}

	public static CraftPlanPacket decode(FriendlyByteBuf buf) {
		boolean started = buf.readBoolean();
		String message = buf.readUtf(256);
		int n = Math.min(buf.readVarInt(), MAX_LINES);
		List<Line> lines = new ArrayList<>(n);
		for (int i = 0; i < n; i++) lines.add(new Line(buf.readItem(), buf.readVarLong(), buf.readByte()));
		return new CraftPlanPacket(started, message, lines);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Minecraft.getInstance().screen instanceof com.robvanblerk.tieredpower.client.screen.CraftRequestScreen s) s.receive(this);
		}));
		ctx.get().setPacketHandled(true);
	}
}
