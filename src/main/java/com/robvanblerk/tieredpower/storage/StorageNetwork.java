package com.robvanblerk.tieredpower.storage;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity;
import com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity;

/**
 * The item storage logic of one network: every disk in every Drive Bay connected to a Storage Controller.
 * Items go first onto disks that already hold that item, then onto the fullest disk with room.
 */
public class StorageNetwork {
	public static final int MAX_BLOCKS = 32_768;

	/** Bumped whenever any disk anywhere changes, so open terminals know to refresh. */
	private static long globalVersion;

	public static void changed() {
		globalVersion++;
	}

	public static long version() {
		return globalVersion;
	}

	public record Stats(boolean online, long used, long capacity, int types, int disks, int bays, long fluidUsed, long fluidCapacity, int fluidTypes,
			int fluidDisks) {
		public static final Stats OFFLINE = new Stats(false, 0, 0, 0, 0, 0, 0, 0, 0, 0);
	}

	/** The special parts a scan finds besides Drive Bays. */
	public static final class Parts {
		public final List<BlockPos> accessPoints = new ArrayList<>();
		public final List<BlockPos> assemblers = new ArrayList<>();
		public final List<BlockPos> cpus = new ArrayList<>();
		public final List<BlockPos> security = new ArrayList<>();
		public final List<BlockPos> matrices = new ArrayList<>();
		public final List<BlockPos> connectors = new ArrayList<>();
		/** Import and Export Buses: the Matrix can also run machines through the machine a bus faces. */
		public final List<BlockPos> buses = new ArrayList<>();
		/** True when the scan hit MAX_BLOCKS and stopped early (parts past that point are missing). */
		public boolean truncated;
	}

	/** Import and Export Buses on this network. */
	public List<BlockPos> buses() {
		return parts.buses;
	}

	/** Whether the last scan stopped early because the network is too big. */
	public boolean truncated() {
		return parts.truncated;
	}

	private final Level level;
	private final List<BlockPos> bays;
	private final List<BlockPos> accessPoints;
	private final Parts parts;

	public StorageNetwork(Level level, List<BlockPos> bays, Parts parts) {
		this.level = level;
		this.bays = bays;
		this.parts = parts;
		this.accessPoints = parts.accessPoints;
	}

	public List<BlockPos> assemblers() {
		return parts.assemblers;
	}

	public List<BlockPos> cpus() {
		return parts.cpus;
	}

	/** How many crafting jobs can run at once: each CPU gives 1, 2, 4 or 8 by tier. */
	public int jobSlots() {
		int n = 0;
		for (BlockPos p : parts.cpus) n += CraftingTier.CPU_JOBS[CraftingTier.of(level.getBlockState(p))];
		return n;
	}

	public List<BlockPos> securityTerminals() {
		return parts.security;
	}

	/** Every pattern in the network's Molecular Assemblers. */
	public List<CraftingPattern> patterns() {
		List<CraftingPattern> out = new ArrayList<>();
		for (BlockPos p : parts.assemblers) {
			if (level.getBlockEntity(p) instanceof com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity a) out.addAll(a.patterns());
		}
		for (BlockPos p : parts.matrices) { // Assembly Matrix patterns
			if (level.getBlockEntity(p) instanceof com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity m) out.addAll(m.patterns());
		}
		return out;
	}

	public List<BlockPos> matrices() {
		return parts.matrices;
	}

	public List<BlockPos> connectors() {
		return parts.connectors;
	}

	/** Stored charged copies of a plain energy item (see ItemKey.isChargedVariant). Empty if the key isn't plain. */
	public List<ItemKey> variantsOf(ItemKey plain) {
		if (!plain.isPlain()) return List.of();
		List<ItemKey> out = new ArrayList<>();
		for (ItemKey k : stock().keySet()) if (k.sameItem(plain) && k.isChargedVariant()) out.add(k);
		return out;
	}

	/** Current stock of every fluid and gas (mB), for planning. */
	public Map<FluidKey, Long> fluidStock() {
		Map<FluidKey, Long> all = new java.util.HashMap<>();
		for (Map.Entry<FluidKey, Long> e : fluidListing()) all.merge(e.getKey(), e.getValue(), Long::sum);
		return all;
	}

	/** Current stock of every item, for planning. */
	public Map<ItemKey, Long> stock() {
		Map<ItemKey, Long> all = new java.util.HashMap<>();
		for (Map.Entry<ItemKey, Long> e : listing()) all.merge(e.getKey(), e.getValue(), Long::sum);
		return all;
	}

	/** Wireless Access Points on this network. */
	public List<BlockPos> accessPoints() {
		return accessPoints;
	}

	public List<BlockPos> bays() {
		return bays;
	}

	/** Finds the controller for a storage block, walking along connected storage blocks. Null if none (or more than one). */
	public static @Nullable StorageControllerBlockEntity findController(Level level, BlockPos start) {
		StorageControllerBlockEntity found = null;
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && seen.size() < MAX_BLOCKS) {
			BlockPos p = queue.poll();
			if (level.getBlockEntity(p) instanceof StorageControllerBlockEntity c) {
				if (found != null) return null; // two controllers: neither works
				found = c;
			}
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (seen.contains(n) || !level.isLoaded(n)) continue;
				if (level.getBlockState(n).getBlock() instanceof StorageNetworkBlock) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		return found;
	}

	/** Walks the network from a controller: drive bays found, and how many controllers (must be 1). */
	public static List<BlockPos> scan(Level level, BlockPos controller, int[] controllers, Parts parts) {
		List<BlockPos> bays = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(controller);
		seen.add(controller);
		controllers[0] = 0;
		while (!queue.isEmpty() && seen.size() < MAX_BLOCKS) {
			BlockPos p = queue.poll();
			var be = level.getBlockEntity(p);
			if (be instanceof StorageControllerBlockEntity) controllers[0]++;
			if (be instanceof DriveBayBlockEntity) bays.add(p);
			var block = level.getBlockState(p).getBlock();
			if (block instanceof com.robvanblerk.tieredpower.block.AccessPointBlock) parts.accessPoints.add(p);
			if (be instanceof com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity) parts.assemblers.add(p);
			if (block instanceof com.robvanblerk.tieredpower.block.CraftingCpuBlock) parts.cpus.add(p);
			if (be instanceof com.robvanblerk.tieredpower.block.entity.SecurityTerminalBlockEntity) parts.security.add(p);
			if (be instanceof com.robvanblerk.tieredpower.block.entity.MatrixControllerBlockEntity) parts.matrices.add(p);
			if (block instanceof com.robvanblerk.tieredpower.block.MachineConnectorBlock) parts.connectors.add(p);
			if (block instanceof com.robvanblerk.tieredpower.block.StorageBusBlock) parts.buses.add(p);
			for (Direction d : Direction.values()) {
				BlockPos n = p.relative(d);
				if (seen.contains(n) || !level.isLoaded(n)) continue;
				if (level.getBlockState(n).getBlock() instanceof StorageNetworkBlock) {
					seen.add(n);
					queue.add(n);
				}
			}
		}
		parts.truncated = !queue.isEmpty();
		return bays;
	}

	private List<DiskInventory> disks() {
		List<DiskInventory> out = new ArrayList<>();
		for (BlockPos p : bays) {
			if (level.getBlockEntity(p) instanceof DriveBayBlockEntity bay) out.addAll(bay.disks());
		}
		return out;
	}

	/** Stores what it can; returns what didn't fit. */
	public ItemStack insert(ItemStack stack, boolean simulate) {
		if (stack.isEmpty()) return ItemStack.EMPTY;
		ItemKey key = new ItemKey(stack);
		long left = stack.getCount();
		List<DiskInventory> disks = disks();
		// Highest priority first; then disks already holding it; then disks partitioned for it; then the fullest.
		disks.sort(Comparator.<DiskInventory>comparingInt(d -> -d.priority())
				.thenComparing(d -> !d.items().containsKey(key))
				.thenComparing(d -> !(d.isPartitioned() && d.accepts(key)))
				.thenComparingLong(d -> d.capacity() - d.used()));
		for (DiskInventory d : disks) {
			if (left <= 0) break;
			left -= d.insert(key, left, simulate);
		}
		return left <= 0 ? ItemStack.EMPTY : stack.copyWithCount((int) left);
	}

	/** Takes up to 'amount' of an item; returns what it got. */
	public ItemStack extract(ItemKey key, int amount, boolean simulate) {
		long got = 0;
		for (DiskInventory d : disks()) {
			if (got >= amount) break;
			got += d.extract(key, amount - got, simulate);
		}
		return got <= 0 ? ItemStack.EMPTY : key.toStack((int) got);
	}

	public long count(ItemKey key) {
		long total = 0;
		for (DiskInventory d : disks()) total += d.items().getOrDefault(key, 0L);
		return total;
	}

	/** Everything stored, combined across disks, most plentiful first. */
	public List<Map.Entry<ItemKey, Long>> listing() {
		Map<ItemKey, Long> all = new LinkedHashMap<>();
		for (DiskInventory d : disks()) for (Map.Entry<ItemKey, Long> e : d.items().entrySet()) all.merge(e.getKey(), e.getValue(), Long::sum);
		List<Map.Entry<ItemKey, Long>> list = new ArrayList<>(all.entrySet());
		list.sort(Map.Entry.<ItemKey, Long>comparingByValue().reversed());
		return list;
	}

	// ---- fluids and gases ----

	private List<FluidDiskInventory> fluidDisks() {
		List<FluidDiskInventory> out = new ArrayList<>();
		for (BlockPos p : bays) {
			if (level.getBlockEntity(p) instanceof DriveBayBlockEntity bay) out.addAll(bay.fluidDisks());
		}
		return out;
	}

	/** Stores what it can; returns how many mB were stored. */
	public int insertFluid(net.minecraftforge.fluids.FluidStack stack, boolean simulate) {
		if (stack.isEmpty()) return 0;
		FluidKey key = new FluidKey(stack);
		long left = stack.getAmount();
		List<FluidDiskInventory> disks = fluidDisks();
		disks.sort(Comparator.<FluidDiskInventory>comparingInt(d -> -d.priority())
				.thenComparing(d -> !d.fluids().containsKey(key))
				.thenComparing(d -> !(d.isPartitioned() && d.accepts(key)))
				.thenComparingLong(d -> d.capacity() - d.used()));
		for (FluidDiskInventory d : disks) {
			if (left <= 0) break;
			left -= d.insert(key, left, simulate);
		}
		return (int) (stack.getAmount() - Math.max(0, left));
	}

	/** Takes up to 'amount' mB of a fluid; returns what it got. */
	public net.minecraftforge.fluids.FluidStack extractFluid(FluidKey key, int amount, boolean simulate) {
		long got = 0;
		for (FluidDiskInventory d : fluidDisks()) {
			if (got >= amount) break;
			got += d.extract(key, amount - got, simulate);
		}
		return got <= 0 ? net.minecraftforge.fluids.FluidStack.EMPTY : key.toStack((int) got);
	}

	public long fluidCount(FluidKey key) {
		long total = 0;
		for (FluidDiskInventory d : fluidDisks()) total += d.fluids().getOrDefault(key, 0L);
		return total;
	}

	/** Every fluid and gas stored, most plentiful first. */
	public List<Map.Entry<FluidKey, Long>> fluidListing() {
		Map<FluidKey, Long> all = new LinkedHashMap<>();
		for (FluidDiskInventory d : fluidDisks()) for (Map.Entry<FluidKey, Long> e : d.fluids().entrySet()) all.merge(e.getKey(), e.getValue(), Long::sum);
		List<Map.Entry<FluidKey, Long>> list = new ArrayList<>(all.entrySet());
		list.sort(Map.Entry.<FluidKey, Long>comparingByValue().reversed());
		return list;
	}

	public Stats stats() {
		long used = 0, cap = 0;
		Set<ItemKey> types = new HashSet<>();
		List<DiskInventory> disks = disks();
		for (DiskInventory d : disks) {
			used += d.used();
			cap += d.capacity();
			types.addAll(d.items().keySet());
		}
		long fUsed = 0, fCap = 0;
		Set<FluidKey> fTypes = new HashSet<>();
		List<FluidDiskInventory> fDisks = fluidDisks();
		for (FluidDiskInventory d : fDisks) {
			fUsed += d.used();
			fCap += d.capacity();
			fTypes.addAll(d.fluids().keySet());
		}
		return new Stats(true, used, cap, types.size(), disks.size(), bays.size(), fUsed, fCap, fTypes.size(), fDisks.size());
	}
}
