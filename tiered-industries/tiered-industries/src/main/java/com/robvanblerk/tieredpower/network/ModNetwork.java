package com.robvanblerk.tieredpower.network;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.network.simple.SimpleChannel;

import com.robvanblerk.tieredpower.TieredPower;

/** Network messages: jetpack jump key (client -> server) and travelling pipe items (server -> client). */
public final class ModNetwork {
	private static final String VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			ResourceLocation.fromNamespaceAndPath(TieredPower.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

	/** Players currently holding jump with a jetpack on (server side). */
	private static final Set<UUID> FLYING = ConcurrentHashMap.newKeySet();

	public static boolean isFlying(UUID player) {
		return FLYING.contains(player);
	}

	public static void stopFlying(UUID player) {
		FLYING.remove(player);
	}

	public record JetpackPacket(boolean flying) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeBoolean(flying);
		}

		public static JetpackPacket decode(FriendlyByteBuf buf) {
			return new JetpackPacket(buf.readBoolean());
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ServerPlayer player = ctx.get().getSender();
			if (player == null) return;
			if (flying) FLYING.add(player.getUUID());
			else FLYING.remove(player.getUUID());
		}
	}

	/** Server -> client: an item travelled along this path of block positions (for the pipe animation). */
	public record PipeItemPacket(ItemStack stack, List<BlockPos> path, int blocksPerSecond) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeItem(stack);
			buf.writeVarInt(blocksPerSecond);
			buf.writeVarInt(path.size());
			for (BlockPos p : path) buf.writeBlockPos(p);
		}

		public static PipeItemPacket decode(FriendlyByteBuf buf) {
			ItemStack stack = buf.readItem();
			int speed = buf.readVarInt();
			int n = Math.min(buf.readVarInt(), 4_096);
			List<BlockPos> path = new ArrayList<>(n);
			for (int i = 0; i < n; i++) path.add(buf.readBlockPos());
			return new PipeItemPacket(stack, path, speed);
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
					() -> () -> com.robvanblerk.tieredpower.client.PipeItemRenderer.add(stack, path, blocksPerSecond));
		}
	}

	public static void sendPipeItem(Level level, BlockPos near, ItemStack stack, List<BlockPos> path, int blocksPerSecond) {
		if (level.isClientSide() || path.size() < 2 || path.size() > 512) return;
		CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(near)), new PipeItemPacket(stack, path, blocksPerSecond));
	}

	// ---------------- Teleporter screen ----------------

	/** One destination in the teleporter screen. */
	public record PadInfo(String dim, BlockPos pos, String name) {}

	/** Server -> client: open the teleporter screen for the pad at 'pos'. */
	public record OpenTeleporterPacket(BlockPos pos, String name, int energy, List<PadInfo> pads, int selected) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeUtf(name, 64);
			buf.writeVarInt(energy);
			buf.writeVarInt(selected);
			buf.writeVarInt(pads.size());
			for (PadInfo p : pads) {
				buf.writeUtf(p.dim(), 128);
				buf.writeBlockPos(p.pos());
				buf.writeUtf(p.name(), 64);
			}
		}

		public static OpenTeleporterPacket decode(FriendlyByteBuf buf) {
			BlockPos pos = buf.readBlockPos();
			String name = buf.readUtf(64);
			int energy = buf.readVarInt();
			int selected = buf.readVarInt();
			int n = Math.min(buf.readVarInt(), 1_000);
			List<PadInfo> pads = new ArrayList<>(n);
			for (int i = 0; i < n; i++) pads.add(new PadInfo(buf.readUtf(128), buf.readBlockPos(), buf.readUtf(64)));
			return new OpenTeleporterPacket(pos, name, energy, pads, selected);
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
					() -> () -> com.robvanblerk.tieredpower.client.TeleporterScreen.open(this));
		}
	}

	/** Client -> server: rename the pad (action 0) or pick destination index 'value' from the list the screen was sent (action 1). */
	public record TeleporterActionPacket(BlockPos pos, int action, String text, String dim, BlockPos target) {
		public void encode(FriendlyByteBuf buf) {
			buf.writeBlockPos(pos);
			buf.writeVarInt(action);
			buf.writeUtf(text, 64);
			buf.writeUtf(dim, 128);
			buf.writeBlockPos(target);
		}

		public static TeleporterActionPacket decode(FriendlyByteBuf buf) {
			return new TeleporterActionPacket(buf.readBlockPos(), buf.readVarInt(), buf.readUtf(64), buf.readUtf(128), buf.readBlockPos());
		}

		public void handle(Supplier<NetworkEvent.Context> ctx) {
			ServerPlayer player = ctx.get().getSender();
			if (player == null || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) return;
			if (!(player.level().getBlockEntity(pos) instanceof com.robvanblerk.tieredpower.block.entity.TeleporterBlockEntity pad)) return;
			if (action == 0) {
				pad.setName(text.trim());
			} else if (action == 1) {
				net.minecraft.resources.ResourceLocation d = net.minecraft.resources.ResourceLocation.tryParse(dim);
				if (d != null) pad.link(d, target);
			} else if (action == 2) {
				pad.unlink();
			}
			openTeleporter(player, pad); // refresh the screen
		}
	}

	public static void openTeleporter(ServerPlayer player, com.robvanblerk.tieredpower.block.entity.TeleporterBlockEntity pad) {
		var registry = com.robvanblerk.tieredpower.multiblock.TeleporterRegistry.get(player.server);
		String here = player.level().dimension().location().toString();
		List<PadInfo> list = new ArrayList<>();
		int selected = -1;
		for (var p : registry.all()) {
			if (p.dim().toString().equals(here) && p.pos().equals(pad.getBlockPos())) continue;
			if (pad.isLinked() && p.dim().equals(pad.getTargetDim()) && p.pos().equals(pad.getTargetPos())) selected = list.size();
			list.add(new PadInfo(p.dim().toString(), p.pos(), p.name()));
		}
		CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
				new OpenTeleporterPacket(pad.getBlockPos(), pad.getName(), pad.energy.getEnergyStored(), list, selected));
	}

	public static void register() {
		CHANNEL.messageBuilder(JetpackPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
				.encoder(JetpackPacket::encode)
				.decoder(JetpackPacket::decode)
				.consumerMainThread(JetpackPacket::handle)
				.add();
		CHANNEL.messageBuilder(OpenTeleporterPacket.class, 2, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(OpenTeleporterPacket::encode)
				.decoder(OpenTeleporterPacket::decode)
				.consumerMainThread(OpenTeleporterPacket::handle)
				.add();
		CHANNEL.messageBuilder(TeleporterActionPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
				.encoder(TeleporterActionPacket::encode)
				.decoder(TeleporterActionPacket::decode)
				.consumerMainThread(TeleporterActionPacket::handle)
				.add();
		CHANNEL.messageBuilder(StorageListPacket.class, 4, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(StorageListPacket::encode)
				.decoder(StorageListPacket::decode)
				.consumerMainThread(StorageListPacket::handle)
				.add();
		CHANNEL.messageBuilder(StorageActionPacket.class, 5, NetworkDirection.PLAY_TO_SERVER)
				.encoder(StorageActionPacket::encode)
				.decoder(StorageActionPacket::decode)
				.consumerMainThread(StorageActionPacket::handle)
				.add();
		CHANNEL.messageBuilder(CraftingFillPacket.class, 6, NetworkDirection.PLAY_TO_SERVER)
				.encoder(CraftingFillPacket::encode)
				.decoder(CraftingFillPacket::decode)
				.consumerMainThread(CraftingFillPacket::handle)
				.add();
		CHANNEL.messageBuilder(OpenWirelessPacket.class, 7, NetworkDirection.PLAY_TO_SERVER)
				.encoder(OpenWirelessPacket::encode)
				.decoder(OpenWirelessPacket::decode)
				.consumerMainThread(OpenWirelessPacket::handle)
				.add();
		CHANNEL.messageBuilder(PatternFillPacket.class, 8, NetworkDirection.PLAY_TO_SERVER)
				.encoder(PatternFillPacket::encode)
				.decoder(PatternFillPacket::decode)
				.consumerMainThread(PatternFillPacket::handle)
				.add();
		CHANNEL.messageBuilder(CraftRequestPacket.class, 9, NetworkDirection.PLAY_TO_SERVER)
				.encoder(CraftRequestPacket::encode)
				.decoder(CraftRequestPacket::decode)
				.consumerMainThread(CraftRequestPacket::handle)
				.add();
		CHANNEL.messageBuilder(CraftPlanPacket.class, 10, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(CraftPlanPacket::encode)
				.decoder(CraftPlanPacket::decode)
				.consumerMainThread(CraftPlanPacket::handle)
				.add();
		CHANNEL.messageBuilder(StockKeeperPacket.class, 11, NetworkDirection.PLAY_TO_SERVER)
				.encoder(StockKeeperPacket::encode)
				.decoder(StockKeeperPacket::decode)
				.consumerMainThread(StockKeeperPacket::handle)
				.add();
		CHANNEL.messageBuilder(JobsRequestPacket.class, 12, NetworkDirection.PLAY_TO_SERVER)
				.encoder(JobsRequestPacket::encode)
				.decoder(JobsRequestPacket::decode)
				.consumerMainThread(JobsRequestPacket::handle)
				.add();
		CHANNEL.messageBuilder(JobsListPacket.class, 13, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(JobsListPacket::encode)
				.decoder(JobsListPacket::decode)
				.consumerMainThread(JobsListPacket::handle)
				.add();
		CHANNEL.messageBuilder(SecurityListPacket.class, 14, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(SecurityListPacket::encode)
				.decoder(SecurityListPacket::decode)
				.consumerMainThread(SecurityListPacket::handle)
				.add();
		CHANNEL.messageBuilder(SecurityActionPacket.class, 15, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SecurityActionPacket::encode)
				.decoder(SecurityActionPacket::decode)
				.consumerMainThread(SecurityActionPacket::handle)
				.add();
		CHANNEL.messageBuilder(MatrixMachinesPacket.class, 16, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(MatrixMachinesPacket::encode)
				.decoder(MatrixMachinesPacket::decode)
				.consumerMainThread(MatrixMachinesPacket::handle)
				.add();
		CHANNEL.messageBuilder(PowerHistoryPacket.class, 17, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(PowerHistoryPacket::encode)
				.decoder(PowerHistoryPacket::decode)
				.consumerMainThread(PowerHistoryPacket::handle)
				.add();
		CHANNEL.messageBuilder(PowerHistoryRequestPacket.class, 18, NetworkDirection.PLAY_TO_SERVER)
				.encoder(PowerHistoryRequestPacket::encode)
				.decoder(PowerHistoryRequestPacket::decode)
				.consumerMainThread(PowerHistoryRequestPacket::handle)
				.add();
		CHANNEL.messageBuilder(SuitModulePacket.class, 19, NetworkDirection.PLAY_TO_SERVER)
				.encoder(SuitModulePacket::encode)
				.decoder(SuitModulePacket::decode)
				.consumerMainThread(SuitModulePacket::handle)
				.add();
		CHANNEL.messageBuilder(ElevatorPacket.class, 21, NetworkDirection.PLAY_TO_SERVER)
				.encoder(ElevatorPacket::encode)
				.decoder(ElevatorPacket::decode)
				.consumerMainThread(ElevatorPacket::handle)
				.add();
		CHANNEL.messageBuilder(LogicValuePacket.class, 20, NetworkDirection.PLAY_TO_SERVER)
				.encoder(LogicValuePacket::encode)
				.decoder(LogicValuePacket::decode)
				.consumerMainThread(LogicValuePacket::handle)
				.add();
		CHANNEL.messageBuilder(StatusListPacket.class, 22, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(StatusListPacket::encode)
				.decoder(StatusListPacket::decode)
				.consumerMainThread(StatusListPacket::handle)
				.add();
		CHANNEL.messageBuilder(PipeItemPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
				.encoder(PipeItemPacket::encode)
				.decoder(PipeItemPacket::decode)
				.consumerMainThread(PipeItemPacket::handle)
				.add();
	}

	private ModNetwork() {}
}
