package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * The sending end of wireless item and fluid transport: pipes, hoppers and buses put items (9 slots) and a fluid or
 * gas (16,000 mB) into it, and every Wireless Receiver linked to it takes them away - any distance, any dimension.
 */
public class WirelessSenderBlockEntity extends BlockEntity {
	public static final int SLOTS = 9, TANK = 16_000;
	public final ItemStackHandler items = new ItemStackHandler(SLOTS) {
		@Override protected void onContentsChanged(int slot) { setChanged(); }
	};
	public final FluidTank tank = new FluidTank(TANK) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> items);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);

	public WirelessSenderBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.WIRELESS_SENDER.get(), pos, state);
	}

	/** A receiver takes up to 'max' of one kind of item (only what 'room' will accept). */
	public ItemStack takeItems(int max, java.util.function.Predicate<ItemStack> fits) {
		for (int i = 0; i < SLOTS; i++) {
			ItemStack peek = items.extractItem(i, max, true);
			if (!peek.isEmpty() && fits.test(peek)) return items.extractItem(i, max, false);
		}
		return ItemStack.EMPTY;
	}

	public int itemCount() {
		int n = 0;
		for (int i = 0; i < SLOTS; i++) n += items.getStackInSlot(i).getCount();
		return n;
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
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		items.deserializeNBT(tag.getCompound("items"));
		tank.readFromNBT(tag.getCompound("tank"));
	}

	public FluidStack takeFluid(FluidStack want, int max) {
		if (tank.isEmpty() || (!want.isEmpty() && !tank.getFluid().isFluidEqual(want))) return FluidStack.EMPTY;
		return tank.drain(max, IFluidHandler.FluidAction.EXECUTE);
	}
}
