package com.robvanblerk.tieredpower.block.entity;

import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.FluidStack;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import com.robvanblerk.tieredpower.block.StorageBusBlock;
import com.robvanblerk.tieredpower.menu.StorageBusMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.ItemKey;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * Import Bus: pulls items from the inventory it faces into the storage network (only filtered items, if any are set).
 * Export Bus: pushes the filtered items from the network into the inventory it faces.
 * Moves up to 8 items every half second.
 */
public class StorageBusBlockEntity extends BlockEntity implements MenuProvider {
	public static final int FILTER_SLOTS = 9, INTERVAL = 10, PER_OPERATION = 8, FLUID_PER_OPERATION = 8_000;

	private final SimpleContainer filter = new SimpleContainer(FILTER_SLOTS) {
		@Override
		public void setChanged() {
			super.setChanged();
			StorageBusBlockEntity.this.setChanged();
		}
	};
	private StorageControllerBlockEntity controller;
	private int ticks, nextFilter;
	private boolean online;

	/** For the screen: 0 = import (1) or export (0), 1 = connected to a powered network. */
	private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
		@Override public int get(int i) { return i == 0 ? (isImport() ? 1 : 0) : (online ? 1 : 0); }
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return 2; }
	};

	public StorageBusBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_BUS.get(), pos, state);
	}

	public boolean isImport() {
		return getBlockState().getBlock() instanceof StorageBusBlock b && b.isImport();
	}

	public SimpleContainer filter() {
		return filter;
	}

	private boolean filterEmpty() {
		for (int i = 0; i < FILTER_SLOTS; i++) if (!filter.getItem(i).isEmpty()) return false;
		return true;
	}

	private boolean passes(ItemStack stack) {
		if (filterEmpty()) return true;
		for (int i = 0; i < FILTER_SLOTS; i++) if (ItemStack.isSameItem(filter.getItem(i), stack)) return true;
		return false;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, StorageBusBlockEntity be) {
		if (++be.ticks % INTERVAL != 0) return;
		if (be.controller == null || be.controller.isRemoved() || be.ticks % 40 == 0) be.controller = StorageNetwork.findController(level, pos);
		StorageNetwork net = be.controller == null ? null : be.controller.getNetwork();
		be.online = net != null;
		if (net == null) return;
		Direction facing = state.getValue(StorageBusBlock.FACING);
		BlockEntity target = level.getBlockEntity(pos.relative(facing));
		if (target == null || target instanceof StorageControllerBlockEntity || target instanceof DriveBayBlockEntity
				|| target instanceof StorageInterfaceBlockEntity) return;
		IItemHandler inv = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).orElse(null);
		IFluidHandler tank = target.getCapability(ForgeCapabilities.FLUID_HANDLER, facing.getOpposite()).orElse(null);
		if (be.isImport()) {
			if (inv != null) be.importFrom(inv, net, pos.relative(facing));
			if (tank != null) be.importFluid(tank, net, pos.relative(facing));
		} else {
			be.export(inv, tank, net);
		}
	}

	/** The fluid a filter slot stands for (a bucket or tank of it), or empty. */
	private static FluidStack filterFluid(ItemStack f) {
		return f.isEmpty() ? FluidStack.EMPTY : FluidUtil.getFluidContained(f).orElse(FluidStack.EMPTY);
	}

	private boolean passesFluid(FluidStack fluid) {
		if (filterEmpty()) return true;
		for (int i = 0; i < FILTER_SLOTS; i++) if (filterFluid(filter.getItem(i)).isFluidEqual(fluid)) return true;
		return false;
	}

	private void importFluid(IFluidHandler tank, StorageNetwork net, BlockPos from) {
		for (int t = 0; t < tank.getTanks(); t++) {
			FluidStack in = tank.getFluidInTank(t);
			if (in.isEmpty() || !passesFluid(in)) continue;
			FluidStack peek = tank.drain(new FluidStack(in, FLUID_PER_OPERATION),
					IFluidHandler.FluidAction.SIMULATE);
			if (peek.isEmpty()) continue;
			int fits = net.insertFluid(peek, true);
			if (fits <= 0) continue;
			FluidStack drained = tank.drain(new FluidStack(in, fits),
					IFluidHandler.FluidAction.EXECUTE);
			int stored = net.insertFluid(drained, false);
			if (stored > 0 && controller != null) controller.creditImported(from, null, new FluidStack(drained, stored));
			return;
		}
	}

	/** Takes turns through the filter: fluid filters send fluid (if the target holds fluids), item filters send items. */
	private void export(@Nullable IItemHandler inv, @Nullable IFluidHandler tank, StorageNetwork net) {
		if (filterEmpty()) return; // an export bus only sends what you've chosen
		for (int tries = 0; tries < FILTER_SLOTS; tries++) {
			ItemStack f = filter.getItem(nextFilter);
			nextFilter = (nextFilter + 1) % FILTER_SLOTS;
			if (f.isEmpty()) continue;
			FluidStack fluid = filterFluid(f);
			if (!fluid.isEmpty() && tank != null && exportFluid(fluid, tank, net)) return;
			if (inv != null && exportItem(f, inv, net)) return;
		}
	}

	private boolean exportFluid(FluidStack fluid, IFluidHandler tank, StorageNetwork net) {
		com.robvanblerk.tieredpower.storage.FluidKey key = new com.robvanblerk.tieredpower.storage.FluidKey(fluid);
		FluidStack offer = net.extractFluid(key, FLUID_PER_OPERATION, true);
		if (offer.isEmpty()) return false;
		int fits = tank.fill(offer, IFluidHandler.FluidAction.SIMULATE);
		if (fits <= 0) return false;
		FluidStack got = net.extractFluid(key, fits, false);
		int filled = tank.fill(got, IFluidHandler.FluidAction.EXECUTE);
		if (filled < got.getAmount()) net.insertFluid(new FluidStack(got, got.getAmount() - filled), false);
		return true;
	}

	private void importFrom(IItemHandler inv, StorageNetwork net, BlockPos from) {
		int moved = 0;
		for (int slot = 0; slot < inv.getSlots() && moved < PER_OPERATION; slot++) {
			ItemStack peek = inv.extractItem(slot, PER_OPERATION - moved, true);
			if (peek.isEmpty() || !passes(peek)) continue;
			int fits = peek.getCount() - net.insert(peek, true).getCount();
			if (fits <= 0) continue;
			ItemStack taken = inv.extractItem(slot, fits, false);
			ItemStack left = net.insert(taken, false);
			if (!left.isEmpty()) ItemHandlerHelper.insertItemStacked(inv, left, false);
			int stored = taken.getCount() - left.getCount();
			if (stored > 0 && controller != null) controller.creditImported(from, taken.copyWithCount(stored), null);
			moved += stored;
		}
	}

	private boolean exportItem(ItemStack f, IItemHandler inv, StorageNetwork net) {
		for (Map.Entry<ItemKey, Long> e : net.listing()) {
			if (!ItemStack.isSameItem(e.getKey().proto(), f)) continue;
			ItemStack offer = net.extract(e.getKey(), PER_OPERATION, true);
			if (offer.isEmpty()) continue;
			int fits = offer.getCount() - ItemHandlerHelper.insertItemStacked(inv, offer, true).getCount();
			if (fits <= 0) continue;
			ItemStack got = net.extract(e.getKey(), fits, false);
			ItemStack left = ItemHandlerHelper.insertItemStacked(inv, got, false);
			if (!left.isEmpty()) net.insert(left, false);
			return true;
		}
		return false;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		for (int i = 0; i < FILTER_SLOTS; i++) {
			ItemStack f = filter.getItem(i);
			if (f.isEmpty()) continue;
			CompoundTag t = f.save(new CompoundTag());
			t.putByte("Slot", (byte) i);
			list.add(t);
		}
		tag.put("Filter", list);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		filter.clearContent();
		net.minecraft.nbt.ListTag list = tag.getList("Filter", net.minecraft.nbt.Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag t = list.getCompound(i);
			int slot = t.getByte("Slot");
			if (slot >= 0 && slot < FILTER_SLOTS) filter.setItem(slot, ItemStack.of(t));
		}
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new StorageBusMenu(id, inv, filter, data);
	}
}
