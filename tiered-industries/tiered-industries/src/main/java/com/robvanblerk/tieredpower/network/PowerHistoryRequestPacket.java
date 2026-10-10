package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import com.robvanblerk.tieredpower.block.entity.PowerMonitorBlockEntity;

/** Client -> server: send me this Power Monitor's history (short or long range). */
public record PowerHistoryRequestPacket(BlockPos pos, boolean longRange) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		buf.writeBoolean(longRange);
	}

	public static PowerHistoryRequestPacket decode(FriendlyByteBuf buf) {
		return new PowerHistoryRequestPacket(buf.readBlockPos(), buf.readBoolean());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 100) return;
			if (player.level().getBlockEntity(pos) instanceof PowerMonitorBlockEntity m)
				ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PowerHistoryPacket(pos, longRange, m.history(longRange)));
		});
		ctx.get().setPacketHandled(true);
	}
}
