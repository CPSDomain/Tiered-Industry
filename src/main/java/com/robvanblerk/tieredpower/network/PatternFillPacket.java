package com.robvanblerk.tieredpower.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.robvanblerk.tieredpower.menu.PatternEncoderMenu;

/** Client -> server: JEI's + button on a Pattern Encoder - a crafting grid, or a machine recipe's inputs and outputs. */
public record PatternFillPacket(int containerId, boolean processing, List<ItemStack> inputs, List<ItemStack> outputs, List<ItemStack> fluidInputs,
		List<ItemStack> fluidOutputs, ItemStack machine) {
	public PatternFillPacket(int containerId, List<ItemStack> craftingGrid) {
		this(containerId, false, craftingGrid, List.of(), List.of(), List.of(), ItemStack.EMPTY);
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(containerId);
		buf.writeBoolean(processing);
		for (int i = 0; i < 9; i++) buf.writeItem(i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY);
		for (int i = 0; i < 3; i++) buf.writeItem(i < outputs.size() ? outputs.get(i) : ItemStack.EMPTY);
		for (int i = 0; i < 3; i++) buf.writeItem(i < fluidInputs.size() ? fluidInputs.get(i) : ItemStack.EMPTY);
		for (int i = 0; i < 3; i++) buf.writeItem(i < fluidOutputs.size() ? fluidOutputs.get(i) : ItemStack.EMPTY);
		buf.writeItem(machine);
	}

	public static PatternFillPacket decode(FriendlyByteBuf buf) {
		int id = buf.readVarInt();
		boolean processing = buf.readBoolean();
		List<ItemStack> ins = new ArrayList<>(9), outs = new ArrayList<>(3), fins = new ArrayList<>(3), fouts = new ArrayList<>(3);
		for (int i = 0; i < 9; i++) ins.add(buf.readItem());
		for (int i = 0; i < 3; i++) outs.add(buf.readItem());
		for (int i = 0; i < 3; i++) fins.add(buf.readItem());
		for (int i = 0; i < 3; i++) fouts.add(buf.readItem());
		ItemStack machine = buf.readItem();
		return new PatternFillPacket(id, processing, ins, outs, fins, fouts, machine);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			var player = ctx.get().getSender();
			if (player != null && player.containerMenu instanceof PatternEncoderMenu menu && menu.containerId == containerId) menu.fill(processing, inputs, outputs, fluidInputs, fluidOutputs, machine);
		});
		ctx.get().setPacketHandled(true);
	}
}
