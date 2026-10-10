package com.robvanblerk.tieredpower.energy;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.robvanblerk.tieredpower.block.FluidPipeBlock;
import com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity;
import com.robvanblerk.tieredpower.item.ItemFilterItem;

/**
 * All connected Item Pipes. Items move instantly (no items travelling visibly through the pipe).
 *  - Anything pushed into a pipe is delivered to the other inventories the network touches (never back into the sender):
 *    highest priority first, equal priorities round-robin, respecting each connection's filter.
 *  - Nearby players see the items travelling along the pipes (a visual - delivery itself is instant, so nothing can be lost).
 *  - Every half second each PULL connection (set with the Wrench) takes up to its tier's items/s ÷ 2 from its block,
 *    only items its filter allows, and delivers them the same way.
 */
public class ItemPipeNetwork {
	public static final int MAX_PIPES = 4_096;

	private final List<ItemPipeBlockEntity> members;
	private final Set<BlockPos> positions = new HashSet<>();
	private boolean valid = true;
	private long lastTick = -1;
	private int roundRobin;

	private ItemPipeNetwork(List<ItemPipeBlockEntity> members) {
		this.members = members;
		for (ItemPipeBlockEntity pipe : members) positions.add(pipe.getBlockPos());
	}

	public boolean isValid() {
		return valid;
	}

	public static ItemPipeNetwork build(Level level, ItemPipeBlockEntity start) {
		List<ItemPipeBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<ItemPipeBlockEntity> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start.getBlockPos());
		while (!queue.isEmpty() && found.size() < MAX_PIPES) {
			ItemPipeBlockEntity pipe = queue.poll();
			found.add(pipe);
			for (Direction dir : Direction.values()) {
				BlockPos next = pipe.getBlockPos().relative(dir);
				if (seen.contains(next) || !level.isLoaded(next)) continue;
				if (pipe.getBlockState().getValue(FluidPipeBlock.SIDES.get(dir)) == PipeSide.DISABLED) continue; // switched off with the Wrench
				if (level.getBlockEntity(next) instanceof ItemPipeBlockEntity other && !other.isRemoved()) {
					seen.add(next);
					queue.add(other);
				}
			}
		}
		ItemPipeNetwork network = new ItemPipeNetwork(found);
		for (ItemPipeBlockEntity pipe : found) pipe.setNetwork(network);
		return network;
	}

	public void invalidate() {
		if (!valid) return;
		valid = false;
		for (ItemPipeBlockEntity pipe : members) if (pipe.getNetwork() == this) pipe.setNetwork(null);
	}

	/** A connection from a pipe to a neighbouring inventory. */
	private record Connection(ItemPipeBlockEntity pipe, Direction side, BlockPos target, IItemHandler handler, ItemStack filter, int priority) {}

	/** True if the last pull found items but nothing on the network would take them. */
	private boolean stuck;

	public boolean isStuck() {
		return stuck;
	}

	public boolean hasDestinations(Level level) {
		return !connections(level, PipeSide.PIPE).isEmpty();
	}

	/** wanted = PIPE for delivering connections (Push, Push + Pull), EXTRACT for pulling ones (Pull, Push + Pull). */
	private List<Connection> connections(Level level, PipeSide wanted) {
		List<Connection> list = new ArrayList<>();
		for (ItemPipeBlockEntity pipe : members) {
			if (pipe.isRemoved()) continue;
			for (Direction dir : Direction.values()) {
				PipeSide mode = pipe.getBlockState().getValue(FluidPipeBlock.SIDES.get(dir));
				if (wanted == PipeSide.PIPE ? !mode.pushes() : !mode.pulls()) continue;
				BlockPos at = pipe.getBlockPos().relative(dir);
				if (!level.isLoaded(at)) continue;
				BlockEntity be = level.getBlockEntity(at);
				if (be == null || be instanceof ItemPipeBlockEntity) continue;
				IItemHandler handler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
				if (handler != null) list.add(new Connection(pipe, dir, at, handler, pipe.getFilter(dir), pipe.getPriority(dir)));
			}
		}
		return list;
	}

	/**
	 * Delivers a stack to the network's destinations (never back into the block at 'from'). Higher-priority connections
	 * are filled first; equal priorities share round-robin. Returns what couldn't be delivered.
	 */
	public ItemStack deliver(Level level, ItemStack stack, BlockPos from, BlockPos sourcePipe, boolean simulate) {
		List<Connection> destinations = connections(level, PipeSide.PIPE);
		if (destinations.isEmpty()) return stack;
		destinations.sort(Comparator.comparingInt(Connection::priority).reversed());
		ItemStack remaining = stack.copy();
		int i = 0;
		while (i < destinations.size() && !remaining.isEmpty()) {
			int priority = destinations.get(i).priority();
			int end = i;
			while (end < destinations.size() && destinations.get(end).priority() == priority) end++;
			int n = end - i;
			int start = Math.floorMod(roundRobin, n);
			for (int k = 0; k < n && !remaining.isEmpty(); k++) {
				Connection c = destinations.get(i + (start + k) % n);
				if (c.target().equals(from) || !ItemFilterItem.passes(c.filter(), remaining)) continue;
				int before = remaining.getCount();
				remaining = ItemHandlerHelper.insertItemStacked(c.handler(), remaining, simulate);
				int moved = before - remaining.getCount();
				if (!simulate && moved > 0) showTravel(level, stack.copyWithCount(moved), from, sourcePipe, c);
			}
			i = end;
		}
		if (!simulate) roundRobin++;
		return remaining;
	}

	/** Sends a "this item travelled along these pipes" animation to nearby players (visual only). */
	private void showTravel(Level level, ItemStack stack, BlockPos from, BlockPos sourcePipe, Connection to) {
		if (!com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.PIPE_ITEM_VISUALS)) return;
		List<BlockPos> path = findPath(sourcePipe, to.pipe().getBlockPos());
		if (path == null) return;
		List<BlockPos> full = new ArrayList<>(path.size() + 2);
		full.add(from);
		full.addAll(path);
		full.add(to.target());
		int speed = Math.max(1, to.pipe().getTier().getItemsPerSecond() >= 512 ? 32 : to.pipe().getTier().getItemsPerSecond() >= 128 ? 16
				: to.pipe().getTier().getItemsPerSecond() >= 32 ? 8 : 4);
		com.robvanblerk.tieredpower.network.ModNetwork.sendPipeItem(level, sourcePipe, stack, full, speed);
	}

	/** Shortest route through the network's pipes (breadth-first). */
	private List<BlockPos> findPath(BlockPos start, BlockPos end) {
		if (!positions.contains(start) || !positions.contains(end)) return null;
		Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		cameFrom.put(start, start);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			if (p.equals(end)) break;
			for (Direction dir : Direction.values()) {
				BlockPos n = p.relative(dir);
				if (positions.contains(n) && !cameFrom.containsKey(n)) {
					cameFrom.put(n, p);
					queue.add(n);
				}
			}
		}
		if (!cameFrom.containsKey(end)) return null;
		List<BlockPos> path = new ArrayList<>();
		for (BlockPos p = end; ; p = cameFrom.get(p)) {
			path.add(0, p);
			if (p.equals(start)) break;
		}
		return path;
	}

	public void tick(Level level) {
		long now = level.getGameTime();
		if (lastTick == now || !valid) return;
		lastTick = now;
		if (now % 10 != 0) return;

		stuck = false;
		for (Connection source : connections(level, PipeSide.EXTRACT)) {
			int budget = Math.max(1, source.pipe().getTier().getItemsPerSecond() / 2);
			IItemHandler from = source.handler();
			for (int slot = 0; slot < from.getSlots() && budget > 0; slot++) {
				ItemStack peek = from.extractItem(slot, budget, true);
				if (peek.isEmpty() || !ItemFilterItem.passes(source.filter(), peek)) continue;
				ItemStack left = deliver(level, peek, source.target(), source.pipe().getBlockPos(), true);
				int movable = peek.getCount() - left.getCount();
				if (movable <= 0) {
					stuck = true;
					continue;
				}
				ItemStack taken = from.extractItem(slot, movable, false);
				ItemStack notDelivered = deliver(level, taken, source.target(), source.pipe().getBlockPos(), false);
				if (!notDelivered.isEmpty()) {
					// Shouldn't happen (we simulated first) - put it back.
					ItemStack back = ItemHandlerHelper.insertItemStacked(from, notDelivered, false);
					if (!back.isEmpty()) net.minecraft.world.Containers.dropItemStack(level,
							source.target().getX() + 0.5, source.target().getY() + 0.5, source.target().getZ() + 0.5, back);
				}
				budget -= taken.getCount() - notDelivered.getCount();
			}
		}
	}
}
