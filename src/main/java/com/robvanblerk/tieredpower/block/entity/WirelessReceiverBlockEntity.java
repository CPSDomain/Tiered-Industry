package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * The receiving end of wireless item and fluid transport: twice a second it takes items (up to 32) and fluid (up to
 * 2,000 mB) from the Wireless Sender it's linked to - any distance, any dimension - and every tick it pushes what it
 * holds into the inventories and tanks next to it. Both ends' chunks must be loaded.
 */
public class WirelessReceiverBlockEntity extends BlockEntity {
	public static final int SLOTS = 9, TANK = 8_000, ITEMS_PER_PULL = 32, FLUID_PER_PULL = 2_000, PULL_EVERY = 10;
	public final ItemStackHandler items = new ItemStackHandler(SLOTS) {
		@Override protected void onContentsChanged(int slot) { setChanged(); }
	};
	public final FluidTank tank = new FluidTank(TANK) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
	private @Nullable BlockPos linkPos;
	private @Nullable ResourceKey<Level> linkDim;
	private int status, ticks; // status: 0 not linked, 1 linked, 2 sender missing or not loaded

	public WirelessReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIRELESS_RECEIVER.get(), pos, state);
	}

	public void link(ResourceKey<Level> dim, BlockPos pos) {
		linkDim = dim;
		linkPos = pos;
		setChanged();
	}

	public int getStatus() { return status; }
	public @Nullable BlockPos getLinkPos() { return linkPos; }

	public static void tick(Level level, BlockPos pos, BlockState state, WirelessReceiverBlockEntity be) {
		if (!(level instanceof ServerLevel server)) return;
		if (++be.ticks % PULL_EVERY == 0) be.pull(server);
		for (int i = 0; i < SLOTS; i++) { // push into neighbours
			ItemStack s = be.items.getStackInSlot(i);
			if (s.isEmpty()) continue;
			ItemStack left = ItemUtil.pushToNeighbours(level, pos, s.copy());
			if (left.getCount() != s.getCount()) be.items.setStackInSlot(i, left);
		}
		if (!be.tank.isEmpty()) {
			FluidStack f = be.tank.getFluid();
			int left = GasTanks.push(level, pos, f.getFluid(), f.getAmount());
			if (left != f.getAmount()) be.tank.drain(f.getAmount() - left, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	private void pull(ServerLevel level) {
		if (linkPos == null || linkDim == null) { status = 0; return; }
		ServerLevel there = level.getServer().getLevel(linkDim);
		if (there == null || !there.isLoaded(linkPos) || !(there.getBlockEntity(linkPos) instanceof WirelessSenderBlockEntity sender)) { status = 2; return; }
		status = 1;
		int moved = 0;
		while (moved < ITEMS_PER_PULL) {
			ItemStack got = sender.takeItems(ITEMS_PER_PULL - moved, s -> ItemHandlerHelper.insertItemStacked(items, s, true).getCount() < s.getCount());
			if (got.isEmpty()) break;
			ItemStack left = ItemHandlerHelper.insertItemStacked(items, got, false);
			moved += got.getCount() - left.getCount();
			if (!left.isEmpty()) { ItemHandlerHelper.insertItemStacked(sender.items, left, false); break; }
		}
		int room = tank.getCapacity() - tank.getFluidAmount();
		if (room > 0) {
			FluidStack got = sender.takeFluid(tank.getFluid(), Math.min(room, FLUID_PER_PULL));
			if (!got.isEmpty()) tank.fill(got, IFluidHandler.FluidAction.EXECUTE);
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); itemCap.invalidate(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); itemCap = LazyOptional.of(() -> items); fluidCap = LazyOptional.of(() -> tank); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("items", items.serializeNBT());
		tag.put("tank", tank.writeToNBT(new CompoundTag()));
		if (linkPos != null && linkDim != null) {
			tag.putLong("linkPos", linkPos.asLong());
			tag.putString("linkDim", linkDim.location().toString());
		}
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items.deserializeNBT(tag.getCompound("items"));
		tank.readFromNBT(tag.getCompound("tank"));
		ResourceLocation dim = tag.contains("linkPos") ? ResourceLocation.tryParse(tag.getString("linkDim")) : null;
		if (dim != null) {
			linkPos = BlockPos.of(tag.getLong("linkPos"));
			linkDim = ResourceKey.create(Registries.DIMENSION, dim);
		}
	}
}
