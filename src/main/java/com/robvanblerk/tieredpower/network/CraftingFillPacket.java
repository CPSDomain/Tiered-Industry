package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.CraftingTerminalMenu;

/** Client -> server: JEI's "+" button on a Crafting Terminal. For each grid slot, the items that would do. */
public record CraftingFillPacket(int containerId, List<List<ItemStack>> slots, boolean max) {
	private static final int MAX_OPTIONS = 32;

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeBoolean(max);
		buf.writeVarInt(slots.size());
		for (List<ItemStack> options : slots) {
			int n = Math.min(options.size(), MAX_OPTIONS);
			buf.writeVarInt(n);
			for (int i = 0; i < n; i++) buf.writeItem(options.get(i));
		}
	}

	public static CraftingFillPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		boolean max = buf.readBoolean();
		int count = Math.min(buf.readVarInt(), 9);
		List<List<ItemStack>> slots = new ArrayList<>();
		for (int s = 0; s < count; s++) {
			int n = Math.min(buf.readVarInt(), MAX_OPTIONS);
			List<ItemStack> options = new ArrayList<>(n);
			for (int i = 0; i < n; i++) options.add(buf.readItem());
			slots.add(options);
		}
		return new CraftingFillPacket(id, slots, max);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof CraftingTerminalMenu menu && menu.containerId == containerId) menu.fillGrid(slots, max);
		});
		ctx.get().setPacketHandled(true);
	}
}
