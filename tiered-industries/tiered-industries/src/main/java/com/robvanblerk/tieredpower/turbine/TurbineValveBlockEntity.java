package com.robvanblerk.tieredpower.turbine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/** Passes steam into its turbine, and lets pipes and cables pull water and power out. */
public class TurbineValveBlockEntity extends BlockEntity {
	private @Nullable BlockPos controllerPos;

	public TurbineValveBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.TURBINE_VALVE.get(), pos, state);
	}

	public void setController(BlockPos pos) {
		if (!pos.equals(controllerPos)) { controllerPos = pos; setChanged(); }
	}

	private @Nullable TurbineControllerBlockEntity controller() {
		if (controllerPos == null || level == null) return null;
		return level.getBlockEntity(controllerPos) instanceof TurbineControllerBlockEntity c && c.isFormed() ? c : null;
	}

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 2; }

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			var c = controller();
			if (c == null) return FluidStack.EMPTY;
			if (tank == 0) return c.getSteam() > 0 ? new FluidStack(ModFluids.STEAM.get(), c.getSteam()) : FluidStack.EMPTY;
			return c.getWater() > 0 ? new FluidStack(Fluids.WATER, c.getWater()) : FluidStack.EMPTY;
		}

		@Override
		public int getTankCapacity(int tank) {
			var c = controller();
			return c == null ? 0 : tank == 0 ? c.steamCapacity() : c.waterCapacity();
		}

		@Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return tank == 0 && stack.getFluid() == ModFluids.STEAM.get(); }

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			var c = controller();
			if (c == null || resource.getFluid() != ModFluids.STEAM.get()) return 0;
			return c.fillSteam(resource.getAmount(), action.simulate());
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			if (resource.getFluid() != Fluids.WATER) return FluidStack.EMPTY;
			return drain(resource.getAmount(), action);
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			var c = controller();
			if (c == null) return FluidStack.EMPTY;
			int n = c.drainWater(maxDrain, action.simulate());
			return n > 0 ? new FluidStack(Fluids.WATER, n) : FluidStack.EMPTY;
		}
	};

	private final IEnergyStorage power = new IEnergyStorage() {
		@Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
		@Override public int extractEnergy(int maxExtract, boolean simulate) { var c = controller(); return c == null ? 0 : c.extractEnergy(maxExtract, simulate); }
		@Override public int getEnergyStored() { var c = controller(); return c == null ? 0 : c.getEnergyStoredInt(); }
		@Override public int getMaxEnergyStored() { var c = controller(); return c == null ? 0 : c.getEnergyCapacityInt(); }
		@Override public boolean canExtract() { return controller() != null; }
		@Override public boolean canReceive() { return false; }
	};

	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> power);

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		fluidCap.invalidate();
		energyCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		fluidCap = LazyOptional.of(() -> fluids);
		energyCap = LazyOptional.of(() -> power);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		if (controllerPos != null) tag.putLong("controller", controllerPos.asLong());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		controllerPos = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
	}
}
