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

/** Server -> client: the network's crafting jobs, for the Jobs screen. */
public record JobsListPacket(List<Job> jobs, int cpus) {
	public record Job(ItemStack item, long amount, int left, String status, List<String> steps) {}

	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(cpus);
		buf.writeVarInt(Math.min(jobs.size(), 50));
		for (int i = 0; i < jobs.size() && i < 50; i++) {
			Job j = jobs.get(i);
			buf.writeItem(j.item());
			buf.writeVarLong(j.amount());
			buf.writeVarInt(j.left());
			buf.writeUtf(j.status(), 200);
			buf.writeVarInt(Math.min(j.steps().size(), 30));
			for (int k = 0; k < j.steps().size() && k < 30; k++) buf.writeUtf(j.steps().get(k), 200);
		}
	}

	public static JobsListPacket decode(FriendlyByteBuf buf) {
		int cpus = buf.readVarInt();
		int n = buf.readVarInt();
		List<Job> jobs = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			ItemStack item = buf.readItem();
			long amount = buf.readVarLong();
			int left = buf.readVarInt();
			String status = buf.readUtf(200);
			int k = buf.readVarInt();
			List<String> steps = new ArrayList<>(k);
			for (int j = 0; j < k; j++) steps.add(buf.readUtf(200));
			jobs.add(new Job(item, amount, left, status, steps));
		}
		return new JobsListPacket(jobs, cpus);
	}

	public void handle(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			if (Minecraft.getInstance().screen instanceof com.robvanblerk.tieredpower.client.screen.JobsScreen s) s.receive(this);
			else if (Minecraft.getInstance().screen instanceof com.robvanblerk.tieredpower.client.screen.StorageTerminalScreen<?> t) t.receiveJobs(this);
		}));
		ctx.get().setPacketHandled(true);
	}
}
