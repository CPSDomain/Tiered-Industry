package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/** Server -> client: everything in the storage network, for an open Storage Terminal. */
public record StorageListPacket(int containerId, StorageNetwork.Stats stats, List<StorageTerminalMenu.Entry> list, List<StorageTerminalMenu.FluidEntry> fluids, List<net.minecraft.world.item.ItemStack> craftables) {
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeBoolean(stats.online());
		buf.writeVarLong(stats.used());
		buf.writeVarLong(stats.capacity());
		buf.writeVarInt(stats.types());
		buf.writeVarInt(stats.disks());
		buf.writeVarInt(stats.bays());
		buf.writeVarLong(stats.fluidUsed());
		buf.writeVarLong(stats.fluidCapacity());
		buf.writeVarInt(stats.fluidTypes());
		buf.writeVarInt(stats.fluidDisks());
		buf.writeVarInt(list.size());
		for (StorageTerminalMenu.Entry e : list) {
			buf.writeItem(e.item());
			buf.writeVarLong(e.count());
		}
		buf.writeVarInt(fluids.size());
		for (StorageTerminalMenu.FluidEntry e : fluids) {
			buf.writeFluidStack(e.fluid());
			buf.writeVarLong(e.amount());
		}
		buf.writeVarInt(craftables.size());
		for (var c : craftables) buf.writeItem(c);
	}

	public static StorageListPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		StorageNetwork.Stats stats = new StorageNetwork.Stats(buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
				buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readVarInt());
		int n = buf.readVarInt();
		List<StorageTerminalMenu.Entry> list = new ArrayList<>(n);
		for (int i = 0; i < n; i++) list.add(new StorageTerminalMenu.Entry(buf.readItem(), buf.readVarLong()));
		int f = buf.readVarInt();
		List<StorageTerminalMenu.FluidEntry> fluids = new ArrayList<>(f);
		for (int i = 0; i < f; i++) fluids.add(new StorageTerminalMenu.FluidEntry(buf.readFluidStack(), buf.readVarLong()));
		int c = buf.readVarInt();
		List<net.minecraft.world.item.ItemStack> craftables = new ArrayList<>(c);
		for (int i = 0; i < c; i++) craftables.add(buf.readItem());
		return new StorageListPacket(id, stats, list, fluids, craftables);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			var player = Minecraft.getInstance().player;
			if (player != null && player.containerMenu instanceof StorageTerminalMenu menu && menu.containerId == containerId) menu.setClientData(stats, list, fluids, craftables);
		}));
		ctx.get().setPacketHandled(true);
	}
}
