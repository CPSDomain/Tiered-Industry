package com.robvanblerk.tieredpower.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.StorageTerminalMenu;

/** Client -> server: a click on the Storage Terminal's item grid. */
public record StorageActionPacket(int containerId, int action, ItemStack item, net.minecraftforge.fluids.FluidStack fluid) {
	public StorageActionPacket(int containerId, int action, ItemStack item) {
		this(containerId, action, item, net.minecraftforge.fluids.FluidStack.EMPTY);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeVarInt(action);
		buf.writeItem(item);
		buf.writeFluidStack(fluid);
	}

	public static StorageActionPacket decode(FriendlyByteBuf buf) {
		return new StorageActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readItem(), buf.readFluidStack());
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof StorageTerminalMenu menu && menu.containerId == containerId) menu.handleAction(action, item, fluid);
		});
		ctx.get().setPacketHandled(true);
	}
}
