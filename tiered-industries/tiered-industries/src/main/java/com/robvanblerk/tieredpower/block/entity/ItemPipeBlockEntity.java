package com.robvanblerk.tieredpower.block.entity;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.block.ItemPipeBlock;
import com.robvanblerk.tieredpower.energy.ItemPipeNetwork;
import com.robvanblerk.tieredpower.energy.ItemPipeTier;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * One Item Pipe segment. Holds the filters installed on its connections. Anything that inserts items into the pipe
 * (a machine with auto-output, a hopper...) has them delivered straight away through the network - never back into
 * the block that inserted them.
 */
public class ItemPipeBlockEntity extends BlockEntity {
	private final ItemPipeTier tier;
	private final Map<Direction, ItemStack> filters = new EnumMap<>(Direction.class);
	private final Map<Direction, Integer> priorities = new EnumMap<>(Direction.class);
	private final Map<Direction, LazyOptional<IItemHandler>> inputs = new EnumMap<>(Direction.class);
	@Nullable
	private ItemPipeNetwork network;

	public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
		this.tier = state.getBlock() instanceof ItemPipeBlock b ? b.getTier() : ItemPipeTier.BASIC;
		makeInputs();
	}

	private void makeInputs() {
		for (Direction dir : Direction.values()) inputs.put(dir, LazyOptional.of(() -> new Input(dir)));
	}

	public ItemPipeTier getTier() {
		return tier;
	}

	public ItemStack getFilter(Direction side) {
		return filters.getOrDefault(side, ItemStack.EMPTY);
	}

	public void setFilter(Direction side, ItemStack filter) {
		if (filter.isEmpty()) filters.remove(side);
		else filters.put(side, filter);
		setChanged();
	}

	public int getPriority(Direction side) {
		return priorities.getOrDefault(side, 0);
	}

	/** Cycles 0..9 and returns the new value. */
	public int cyclePriority(Direction side) {
		int next = (getPriority(side) + 1) % 10;
		if (next == 0) priorities.remove(side);
		else priorities.put(side, next);
		setChanged();
		return next;
	}

	public void dropFilters(Level level, BlockPos pos) {
		for (ItemStack f : filters.values()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, f);
		filters.clear();
	}

	public @Nullable ItemPipeNetwork getNetwork() {
		return network;
	}

	public void setNetwork(@Nullable ItemPipeNetwork network) {
		this.network = network;
	}

	public ItemPipeNetwork network() {
		if ((network == null || !network.isValid()) && level != null) ItemPipeNetwork.build(level, this);
		return network;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
		ItemPipeNetwork net = be.network();
		if (net != null) net.tick(level);
		if (level.getGameTime() % 20 == 0) {
			BlockState updated = ItemPipeBlock.withConnections(state, level, pos);
			if (updated != state) level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
		}
	}

	/** What a neighbour on one side sees: a single slot that delivers whatever is put in, anywhere else on the network. */
	private class Input implements IItemHandler {
		private final Direction side;

		Input(Direction side) {
			this.side = side;
		}

		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public @NotNull ItemStack getStackInSlot(int slot) {
			return ItemStack.EMPTY;
		}

		@Override
		public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
			if (stack.isEmpty() || level == null || level.isClientSide()) return stack;
			ItemPipeNetwork net = network();
			if (net == null) return stack;
			return net.deliver(level, stack, worldPosition.relative(side), worldPosition, simulate);
		}

		@Override
		public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, @NotNull ItemStack stack) {
			return true;
		}
	}

	private void invalidateNeighbours() {
		if (level == null) return;
		if (network != null) network.invalidate();
		for (Direction dir : Direction.values()) {
			BlockPos next = worldPosition.relative(dir);
			if (!level.isLoaded(next)) continue;
			if (level.getBlockEntity(next) instanceof ItemPipeBlockEntity other && other.network != null) other.network.invalidate();
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && !level.isClientSide()) invalidateNeighbours();
	}

	@Override
	public void setRemoved() {
		if (level != null && !level.isClientSide()) invalidateNeighbours();
		super.setRemoved();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) return inputs.get(side).cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		inputs.values().forEach(LazyOptional::invalidate);
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		makeInputs();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		CompoundTag f = new CompoundTag();
		filters.forEach((dir, stack) -> f.put(dir.getName(), stack.save(new CompoundTag())));
		tag.put("filters", f);
		CompoundTag pr = new CompoundTag();
		priorities.forEach((dir, value) -> pr.putInt(dir.getName(), value));
		tag.put("priorities", pr);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		filters.clear();
		CompoundTag f = tag.getCompound("filters");
		for (Direction dir : Direction.values()) {
			if (f.contains(dir.getName())) filters.put(dir, ItemStack.of(f.getCompound(dir.getName())));
		}
		priorities.clear();
		CompoundTag pr = tag.getCompound("priorities");
		for (Direction dir : Direction.values()) {
			if (pr.contains(dir.getName())) priorities.put(dir, pr.getInt(dir.getName()));
		}
	}
}
