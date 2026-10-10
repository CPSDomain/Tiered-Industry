package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.storage.StorageNetwork;

/**
 * A doorway into the storage network from anywhere on its cable: machines, pipes and reactors next to it can push items,
 * fluids and gases in, or pull them out, as if it were one huge chest and tank.
 */
public class StorageInterfaceBlockEntity extends BlockEntity {
	private StorageControllerBlockEntity controller;
	private long lastLookup = Long.MIN_VALUE;

	public StorageInterfaceBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STORAGE_INTERFACE.get(), pos, state);
	}

	private @Nullable StorageControllerBlockEntity controller() {
		if (level == null || level.isClientSide()) return null;
		long now = level.getGameTime();
		if (controller == null || controller.isRemoved() || now - lastLookup >= 40) {
			controller = StorageNetwork.findController(level, worldPosition);
			lastLookup = now;
		}
		return controller != null && controller.isOnline() ? controller : null;
	}

	private final IItemHandler items = new IItemHandler() {
		@Override public int getSlots() { var c = controller(); return c == null ? 1 : c.itemHandler().getSlots(); }
		@Override public @NotNull ItemStack getStackInSlot(int s) { var c = controller(); return c == null ? ItemStack.EMPTY : c.itemHandler().getStackInSlot(s); }
		@Override public @NotNull ItemStack insertItem(int s, @NotNull ItemStack st, boolean sim) { var c = controller(); return c == null ? st : c.itemHandler().insertItem(s, st, sim); }
		@Override public @NotNull ItemStack extractItem(int s, int n, boolean sim) { var c = controller(); return c == null ? ItemStack.EMPTY : c.itemHandler().extractItem(s, n, sim); }
		@Override public int getSlotLimit(int s) { return 64; }
		@Override public boolean isItemValid(int s, @NotNull ItemStack st) { return true; }
	};

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { var c = controller(); return c == null ? 1 : c.fluidHandler().getTanks(); }
		@Override public @NotNull FluidStack getFluidInTank(int t) { var c = controller(); return c == null ? FluidStack.EMPTY : c.fluidHandler().getFluidInTank(t); }
		@Override public int getTankCapacity(int t) { return Integer.MAX_VALUE; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack st) { return true; }
		@Override public int fill(FluidStack r, FluidAction a) { var c = controller(); return c == null ? 0 : c.fluidHandler().fill(r, a); }
		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { var c = controller(); return c == null ? FluidStack.EMPTY : c.fluidHandler().drain(r, a); }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { var c = controller(); return c == null ? FluidStack.EMPTY : c.fluidHandler().drain(m, a); }
	};

	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) return itemCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		itemCap.invalidate();
		fluidCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		itemCap = LazyOptional.of(() -> items);
		fluidCap = LazyOptional.of(() -> fluids);
	}
}
