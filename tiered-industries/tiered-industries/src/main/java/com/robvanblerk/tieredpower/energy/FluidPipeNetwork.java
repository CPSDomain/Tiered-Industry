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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.FluidPipeBlock;
import com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity;

/**
 * All connected pipes of one kind form a network carrying one fluid at a time. Once per tick it:
 *   1. pools the fluid in every segment,
 *   2. pulls from neighbours on EXTRACT connections (set with the Wrench),
 *   3. shares the pool out to every other neighbour that accepts it (each limited to the touching pipe's tier rate),
 *   4. spreads what's left back over the pipes.
 * When the network empties completely it can carry a different fluid.
 */
public class FluidPipeNetwork {
	public static final int MAX_PIPES = 4_096;

	private final List<FluidPipeBlockEntity> members;
	private FluidStack carrying = FluidStack.EMPTY;
	private long lastTick = -1;
	private boolean valid = true;

	private FluidPipeNetwork(List<FluidPipeBlockEntity> members) {
		this.members = members;
	}

	public FluidStack getCarrying() {
		return carrying;
	}

	public boolean isValid() {
		return valid;
	}

	public static FluidPipeNetwork build(Level level, FluidPipeBlockEntity start) {
		List<FluidPipeBlockEntity> found = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<FluidPipeBlockEntity> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start.getBlockPos());
		while (!queue.isEmpty() && found.size() < MAX_PIPES) {
			FluidPipeBlockEntity pipe = queue.poll();
			found.add(pipe);
			for (Direction dir : Direction.values()) {
				BlockPos next = pipe.getBlockPos().relative(dir);
				if (seen.contains(next) || !level.isLoaded(next)) continue;
				if (pipe.getBlockState().getValue(FluidPipeBlock.SIDES.get(dir)) == PipeSide.DISABLED) continue; // switched off with the Wrench
				if (level.getBlockEntity(next) instanceof FluidPipeBlockEntity other && !other.isRemoved() && other.getKind() == start.getKind()) {
					seen.add(next);
					queue.add(other);
				}
			}
		}
		FluidPipeNetwork network = new FluidPipeNetwork(found);
		for (FluidPipeBlockEntity pipe : found) {
			if (network.carrying.isEmpty() && !pipe.tank.isEmpty()) network.carrying = new FluidStack(pipe.tank.getFluid(), 1);
			pipe.setNetwork(network);
		}
		return network;
	}

	public void invalidate() {
		if (!valid) return;
		valid = false;
		for (FluidPipeBlockEntity pipe : members) {
			if (pipe.getNetwork() == this) pipe.setNetwork(null);
		}
	}

	private record Link(IFluidHandler handler, int limit, net.minecraft.world.item.ItemStack filter) {}

	public void tick(Level level) {
		long now = level.getGameTime();
		if (lastTick == now || !valid) return;
		lastTick = now;

		long pool = 0, bufferSize = 0;
		for (FluidPipeBlockEntity pipe : members) {
			if (pipe.isRemoved()) continue;
			bufferSize += pipe.getTier().getRate();
			FluidStack held = pipe.tank.getFluid();
			if (held.isEmpty()) continue;
			if (carrying.isEmpty()) carrying = new FluidStack(held, 1);
			if (held.isFluidEqual(carrying)) pool += held.getAmount();
		}

		List<Link> consumers = new ArrayList<>();
		List<Link> sources = new ArrayList<>();
		for (FluidPipeBlockEntity pipe : members) {
			if (pipe.isRemoved()) continue;
			BlockPos pos = pipe.getBlockPos();
			for (Direction dir : Direction.values()) {
				PipeSide side = pipe.getBlockState().getValue(FluidPipeBlock.SIDES.get(dir));
				if (side == PipeSide.NONE || side == PipeSide.DISABLED) continue;
				BlockPos at = pos.relative(dir);
				if (!level.isLoaded(at)) continue;
				BlockEntity neighbour = level.getBlockEntity(at);
				if (neighbour == null || neighbour instanceof FluidPipeBlockEntity) continue;
				IFluidHandler handler = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (handler == null) continue;
				Link link = new Link(handler, pipe.getTier().getRate(), pipe.getFilter(dir));
				if (side.pulls()) sources.add(link);
				if (side.pushes()) consumers.add(link);
			}
		}

		// Pull from EXTRACT connections.
		for (Link source : sources) {
			long room = bufferSize - pool;
			if (room <= 0) break;
			int amount = (int) Math.min(source.limit(), room);
			FluidStack offer = FluidStack.EMPTY;
			if (!carrying.isEmpty()) {
				offer = source.handler().drain(new FluidStack(carrying, amount), IFluidHandler.FluidAction.SIMULATE);
			} else {
				// An allow-list filter asks for its fluids by name (a tank may hold several); otherwise take whatever comes.
				for (FluidStack want : com.robvanblerk.tieredpower.item.FluidFilterItem.wanted(source.filter())) {
					offer = source.handler().drain(new FluidStack(want, amount), IFluidHandler.FluidAction.SIMULATE);
					if (!offer.isEmpty()) break;
				}
				if (offer.isEmpty()) offer = source.handler().drain(amount, IFluidHandler.FluidAction.SIMULATE);
			}
			if (offer.isEmpty() || !members.get(0).getKind().accepts(offer)) continue;
			if (!com.robvanblerk.tieredpower.item.FluidFilterItem.passes(source.filter(), offer)) continue;
			FluidStack drained = source.handler().drain(offer, IFluidHandler.FluidAction.EXECUTE);
			if (drained.isEmpty()) continue;
			if (carrying.isEmpty()) carrying = new FluidStack(drained, 1);
			pool += drained.getAmount();
		}

		// Share out to everything else that accepts it.
		if (pool > 0 && !consumers.isEmpty()) {
			int[] given = new int[consumers.size()];
			long share = Math.max(1, pool / consumers.size());
			for (int i = 0; i < consumers.size() && pool > 0; i++) {
				Link c = consumers.get(i);
				if (!com.robvanblerk.tieredpower.item.FluidFilterItem.passes(c.filter(), carrying)) continue;
				int offer = (int) Math.min(Math.min(share, c.limit()), pool);
				int filled = c.handler().fill(new FluidStack(carrying, offer), IFluidHandler.FluidAction.EXECUTE);
				given[i] += filled;
				pool -= filled;
			}
			for (int i = 0; i < consumers.size() && pool > 0; i++) {
				Link c = consumers.get(i);
				if (!com.robvanblerk.tieredpower.item.FluidFilterItem.passes(c.filter(), carrying)) continue;
				int offer = (int) Math.min(c.limit() - given[i], pool);
				if (offer <= 0) continue;
				pool -= c.handler().fill(new FluidStack(carrying, offer), IFluidHandler.FluidAction.EXECUTE);
			}
		}

		// Spread what's left back over the pipes (pipes stuck holding some other fluid are left alone).
		long remaining = pool;
		for (FluidPipeBlockEntity pipe : members) {
			if (pipe.isRemoved()) continue;
			FluidStack held = pipe.tank.getFluid();
			if (!held.isEmpty() && !held.isFluidEqual(carrying)) continue;
			int rate = pipe.getTier().getRate();
			int amount = bufferSize <= 0 ? 0 : (int) Math.min(rate, Math.min(remaining, pool * rate / bufferSize));
			pipe.tank.setFluid(amount > 0 ? new FluidStack(carrying, amount) : FluidStack.EMPTY);
			remaining -= amount;
		}
		for (FluidPipeBlockEntity pipe : members) {
			if (remaining <= 0) break;
			if (pipe.isRemoved()) continue;
			FluidStack held = pipe.tank.getFluid();
			if (!held.isEmpty() && !held.isFluidEqual(carrying)) continue;
			int add = (int) Math.min(pipe.getTier().getRate() - held.getAmount(), remaining);
			if (add <= 0) continue;
			pipe.tank.setFluid(new FluidStack(carrying, held.getAmount() + add));
			remaining -= add;
		}
		for (FluidPipeBlockEntity pipe : members) if (!pipe.isRemoved()) pipe.setChanged();

		// Completely empty: free to carry a different fluid next time.
		if (pool <= 0 && remaining <= 0 && members.stream().allMatch(p -> p.tank.isEmpty())) carrying = FluidStack.EMPTY;
	}
}
