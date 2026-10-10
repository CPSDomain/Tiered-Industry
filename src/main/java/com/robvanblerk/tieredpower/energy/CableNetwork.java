package com.robvanblerk.tieredpower.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.entity.CableBlockEntity;

/**
 * All connected cables form one network. Once per tick the network:
 *   1. pools the energy held in every cable segment,
 *   2. pulls from neighbouring pure generators (blocks that output but don't accept power),
 *   3. shares the pool out fairly to every neighbouring block that accepts power, anywhere on the network
 *      (each connection limited by the tier rate of the cable touching it),
 *   4. spreads what's left back across the cables.
 * So a long cable run carries the same amount as a short one.
 */
public class CableNetwork {
	/** Safety limit so a giant accidental network can't lag the server. */
	public static final int MAX_CABLES = 4_096;

	private final List<CableBlockEntity> members;
	private long lastTick = -1;
	private boolean valid = true;

	/** Averages over the last second, for the Multimeter and Power Monitor. */
	public record Stats(long in, long toMachines, long toStorage, long stored, long capacity, int cables, int generators, int machines, int storages) {
		public static final Stats EMPTY = new Stats(0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	private static final int AVERAGE_TICKS = 20;
	private Stats stats = Stats.EMPTY;
	private long leftOver = -1; // energy left in the cables after the last tick
	private long sumIn, sumMachines, sumStorage;
	private int ticksCounted;

	private CableNetwork(List<CableBlockEntity> members) {
		this.members = members;
	}

	/** Finds every cable connected to start and links them all to one new network. */
	public static CableNetwork build(Level level, CableBlockEntity start) {
		List<CableBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<CableBlockEntity> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start.getBlockPos());
		while (!queue.isEmpty() && found.size() < MAX_CABLES) {
			CableBlockEntity cable = queue.poll();
			found.add(cable);
			for (Direction dir : Direction.values()) {
				BlockPos next = cable.getBlockPos().relative(dir);
				if (seen.contains(next) || !level.isLoaded(next)) continue;
				if (level.getBlockEntity(next) instanceof CableBlockEntity other && !other.isRemoved()) {
					seen.add(next);
					queue.add(other);
				}
			}
		}
		CableNetwork network = new CableNetwork(found);
		for (CableBlockEntity cable : found) cable.setNetwork(network);
		return network;
	}

	/** Called when cables are added or removed: every member will rebuild its network on its next tick. */
	public void invalidate() {
		if (!valid) return;
		valid = false;
		for (CableBlockEntity cable : members) {
			if (cable.getNetwork() == this) cable.setNetwork(null);
		}
	}

	public boolean isValid() {
		return valid;
	}

	public Stats getStats() {
		return stats;
	}

	private record Link(IEnergyStorage storage, int limit, boolean isStorage, BlockPos at) {}

	/** Runs the network once per game tick, whichever member ticks first. */
	public void tick(Level level) {
		long now = level.getGameTime();
		if (lastTick == now || !valid) return;
		lastTick = now;

		List<Link> consumers = new ArrayList<>();
		List<Link> sources = new ArrayList<>();
		long pool = 0, bufferSize = 0;

		for (CableBlockEntity cable : members) {
			if (cable.isRemoved()) continue;
			int rate = cable.getTier().getTransferRate();
			pool += cable.energy.getEnergyStored();
			bufferSize += rate;
			for (Direction dir : Direction.values()) {
				BlockPos at = cable.getBlockPos().relative(dir);
				if (!level.isLoaded(at)) continue;
				BlockEntity neighbour = level.getBlockEntity(at);
				if (neighbour == null || neighbour instanceof CableBlockEntity) continue;
				IEnergyStorage storage = neighbour.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
				if (storage == null) continue;
				if (storage.canReceive()) consumers.add(new Link(storage, rate, storage.canExtract(), at));
				else if (storage.canExtract()) sources.add(new Link(storage, rate, false, at));
			}
		}

		// Energy generators and batteries pushed into the cables since last tick.
		long in = leftOver < 0 ? 0 : Math.max(0, pool - leftOver);

		// Pull from generators that don't push on their own.
		for (Link source : sources) {
			long room = bufferSize - pool;
			if (room <= 0) break;
			int pulled = source.storage().extractEnergy((int) Math.min(source.limit(), room), false);
			pool += pulled;
			in += pulled;
		}
		long toMachines = 0, toStorage = 0;

		// Share out: first an even split, then top up anyone who can take more.
		if (!consumers.isEmpty() && pool > 0) {
			int[] given = new int[consumers.size()];
			long share = Math.max(1, pool / consumers.size());
			for (int i = 0; i < consumers.size() && pool > 0; i++) {
				Link c = consumers.get(i);
				int offer = (int) Math.min(Math.min(share, c.limit()), pool);
				int accepted = c.storage().receiveEnergy(offer, false);
				given[i] += accepted;
				pool -= accepted;
				if (c.isStorage()) toStorage += accepted; else toMachines += accepted;
			}
			for (int i = 0; i < consumers.size() && pool > 0; i++) {
				Link c = consumers.get(i);
				int offer = (int) Math.min(c.limit() - given[i], pool);
				if (offer <= 0) continue;
				int accepted = c.storage().receiveEnergy(offer, false);
				pool -= accepted;
				if (c.isStorage()) toStorage += accepted; else toMachines += accepted;
			}
		}

		// Spread what's left back over the cables (in proportion to their size).
		long remaining = pool;
		for (CableBlockEntity cable : members) {
			if (cable.isRemoved()) continue;
			int rate = cable.getTier().getTransferRate();
			int amount = bufferSize <= 0 ? 0 : (int) Math.min(rate, pool * rate / bufferSize);
			cable.energy.setEnergy(amount);
			remaining -= amount;
		}
		for (CableBlockEntity cable : members) {
			if (remaining <= 0) break;
			if (cable.isRemoved()) continue;
			int space = cable.energy.getSpace();
			int add = (int) Math.min(space, remaining);
			cable.energy.setEnergy(cable.energy.getEnergyStored() + add);
			remaining -= add;
		}
		long left = 0;
		for (CableBlockEntity cable : members) {
			if (cable.isRemoved()) continue;
			cable.setChanged();
			left += cable.energy.getEnergyStored();
		}
		leftOver = left;

		sumIn += in;
		sumMachines += toMachines;
		sumStorage += toStorage;
		if (++ticksCounted >= AVERAGE_TICKS) {
			publishStats(consumers, sources);
			sumIn = sumMachines = sumStorage = 0;
			ticksCounted = 0;
		}
	}

	private void publishStats(List<Link> consumers, List<Link> sources) {
		// The same battery or machine can touch several cables: count each once.
		Set<BlockPos> seen = new HashSet<>();
		long stored = 0, capacity = 0;
		int machines = 0, storages = 0, generators = 0;
		for (Link c : consumers) {
			if (!seen.add(c.at())) continue;
			if (c.isStorage()) {
				storages++;
				stored += c.storage().getEnergyStored();
				capacity += c.storage().getMaxEnergyStored();
			} else {
				machines++;
			}
		}
		for (Link g : sources) if (seen.add(g.at())) generators++;
		stats = new Stats(sumIn / AVERAGE_TICKS, sumMachines / AVERAGE_TICKS, sumStorage / AVERAGE_TICKS, stored, capacity,
				members.size(), generators, machines, storages);
		// Light the cables while power is moving through them (their glowing core); dark when idle.
		boolean flowing = stats.toMachines() + stats.toStorage() > 0;
		for (CableBlockEntity c : members) {
			if (c.isRemoved() || c.getLevel() == null) continue;
			var st = c.getBlockState();
			if (st.hasProperty(com.robvanblerk.tieredpower.block.CableBlock.POWERED) && st.getValue(com.robvanblerk.tieredpower.block.CableBlock.POWERED) != flowing)
				c.getLevel().setBlock(c.getBlockPos(), st.setValue(com.robvanblerk.tieredpower.block.CableBlock.POWERED, flowing),
						net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
		}
	}
}
