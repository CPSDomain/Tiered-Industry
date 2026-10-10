package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.energy.CableNetwork;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Place it touching any cable: its screen shows the whole network - power in, power to machines, power into batteries and
 * how full they are. Comparator: how full the network's batteries are (0-15).
 */
public class PowerMonitorBlockEntity extends BlockEntity {
	private CableNetwork.Stats stats = CableNetwork.Stats.EMPTY;
	private boolean connected;
	private int ticks;

	// ---- history for the graph screen: last 10 minutes (every 5 s) and last 2 hours (every minute) ----
	public static final int SAMPLES = 120, SHORT_EVERY = 100, LONG_EVERY = 1200;
	/** Each sample: generated FE/t, used FE/t, battery fill in thousandths. Oldest first. */
	private final java.util.ArrayDeque<long[]> shortHistory = new java.util.ArrayDeque<>(), longHistory = new java.util.ArrayDeque<>();
	private long historyTicks;

	public java.util.List<long[]> history(boolean longRange) {
		return new java.util.ArrayList<>(longRange ? longHistory : shortHistory);
	}

	private void sample(java.util.ArrayDeque<long[]> into) {
		long fill = stats.capacity() <= 0 ? 0 : stats.stored() * 1000 / stats.capacity();
		into.addLast(new long[]{stats.in(), stats.toMachines() + stats.toStorage(), fill});
		while (into.size() > SAMPLES) into.removeFirst();
	}

	public PowerMonitorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.POWER_MONITOR.get(), pos, state);
	}

	public CableNetwork.Stats getStats() { return stats; }
	public boolean isConnected() { return connected; }

	/** The network of the first cable touching this block, if any. */
	public static @Nullable CableNetwork findNetwork(Level level, BlockPos pos) {
		for (Direction dir : Direction.values()) {
			if (level.getBlockEntity(pos.relative(dir)) instanceof CableBlockEntity cable && cable.getNetwork() != null && cable.getNetwork().isValid())
				return cable.getNetwork();
		}
		return null;
	}

	public int comparatorSignal() {
		if (stats.capacity() <= 0 || stats.stored() <= 0) return 0;
		return Math.max(1, (int) Math.floor(15.0 * stats.stored() / stats.capacity()));
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PowerMonitorBlockEntity be) {
		if (!level.isClientSide()) {
			be.historyTicks++;
			if (be.historyTicks % SHORT_EVERY == 0) be.sample(be.shortHistory);
			if (be.historyTicks % LONG_EVERY == 0) { be.sample(be.longHistory); be.setChanged(); }
		}
		if (++be.ticks < 20) return;
		be.ticks = 0;
		CableNetwork network = findNetwork(level, pos);
		int oldSignal = be.comparatorSignal();
		CableNetwork.Stats now = network == null ? CableNetwork.Stats.EMPTY : network.getStats();
		boolean nowConnected = network != null;
		if (!now.equals(be.stats) || nowConnected != be.connected) {
			be.stats = now;
			be.connected = nowConnected;
			be.setChanged();
			level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
		}
		if (be.comparatorSignal() != oldSignal) level.updateNeighbourForOutputSignal(pos, state.getBlock());
	}

	private void write(CompoundTag tag) {
		tag.putBoolean("connected", connected);
		tag.putLong("in", stats.in());
		tag.putLong("machines", stats.toMachines());
		tag.putLong("storage", stats.toStorage());
		tag.putLong("stored", stats.stored());
		tag.putLong("capacity", stats.capacity());
		tag.putInt("cables", stats.cables());
		tag.putInt("generators", stats.generators());
		tag.putInt("machineCount", stats.machines());
		tag.putInt("storages", stats.storages());
	}

	private void read(CompoundTag tag) {
		connected = tag.getBoolean("connected");
		stats = new CableNetwork.Stats(tag.getLong("in"), tag.getLong("machines"), tag.getLong("storage"), tag.getLong("stored"),
				tag.getLong("capacity"), tag.getInt("cables"), tag.getInt("generators"), tag.getInt("machineCount"), tag.getInt("storages"));
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		write(tag);
		tag.put("shortHistory", saveHistory(shortHistory));
		tag.put("longHistory", saveHistory(longHistory));
	}
	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		read(tag);
		loadHistory(shortHistory, tag.getList("shortHistory", net.minecraft.nbt.Tag.TAG_LONG_ARRAY));
		loadHistory(longHistory, tag.getList("longHistory", net.minecraft.nbt.Tag.TAG_LONG_ARRAY));
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag tag = super.getUpdateTag();
		write(tag);
		return tag;
	}

	@Override public void handleUpdateTag(CompoundTag tag) { read(tag); }
	@Override public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

	private static net.minecraft.nbt.ListTag saveHistory(java.util.ArrayDeque<long[]> h) {
		var list = new net.minecraft.nbt.ListTag();
		for (long[] v : h) list.add(new net.minecraft.nbt.LongArrayTag(v));
		return list;
	}

	private static void loadHistory(java.util.ArrayDeque<long[]> into, net.minecraft.nbt.ListTag list) {
		into.clear();
		for (int i = 0; i < list.size(); i++) {
			long[] v = ((net.minecraft.nbt.LongArrayTag) list.get(i)).getAsLongArray();
			if (v.length == 3) into.addLast(v);
		}
	}
}
