package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Server -> client: a Power Monitor's history (generated, used, fill per sample). Opens or updates the graph screen. */
public record PowerHistoryPacket(BlockPos pos, boolean longRange, List<long[]> samples) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeBoolean(longRange);
		buf.writeVarInt(samples.size());
		for (long[] s : samples) { buf.writeVarLong(s[0]); buf.writeVarLong(s[1]); buf.writeVarLong(s[2]); }
	}

	public static PowerHistoryPacket decode(FriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		boolean lr = buf.readBoolean();
		int n = Math.min(buf.readVarInt(), 512);
		List<long[]> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(new long[]{buf.readVarLong(), buf.readVarLong(), buf.readVarLong()});
		return new PowerHistoryPacket(pos, lr, list);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var mc = Minecraft.getInstance();
			if (mc.screen instanceof com.robvanblerk.tieredpower.client.screen.PowerHistoryScreen s && s.pos().equals(pos)) s.receive(longRange, samples);
			else mc.setScreen(new com.robvanblerk.tieredpower.client.screen.PowerHistoryScreen(pos, longRange, samples));
		}));
		ctx.get().setPacketHandled(true);
	}
}
