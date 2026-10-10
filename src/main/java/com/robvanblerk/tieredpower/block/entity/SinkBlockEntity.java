package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/** An endless water source. Anything can drain water from it, and it pushes water into neighbours each tick. */
public class SinkBlockEntity extends BlockEntity {
	private static final IFluidHandler INFINITE_WATER = new IFluidHandler() {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			return new FluidStack(Fluids.WATER, Integer.MAX_VALUE);
		}

		@Override
		public int getTankCapacity(int tank) {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
			return false;
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			return 0;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return resource.getFluid() == Fluids.WATER ? resource.copy() : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			return maxDrain <= 0 ? FluidStack.EMPTY : new FluidStack(Fluids.WATER, maxDrain);
		}
	};

	private LazyOptional<IFluidHandler> cap = LazyOptional.of(() -> INFINITE_WATER);

	public SinkBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SINK.get(), pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SinkBlockEntity be) {
		int amount = Config.get(Config.SINK_WATER_PER_TICK);
		for (Direction dir : Direction.values()) {
			BlockPos next = pos.relative(dir);
			if (!level.isLoaded(next)) continue;
			BlockEntity neighbour = level.getBlockEntity(next);
			if (neighbour == null || neighbour instanceof SinkBlockEntity) continue;
			neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).ifPresent(target ->
					FluidUtil.tryFluidTransfer(target, INFINITE_WATER, new FluidStack(Fluids.WATER, amount), true));
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
		if (capability == ForgeCapabilities.FLUID_HANDLER) return cap.cast();
		return super.getCapability(capability, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		cap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		cap = LazyOptional.of(() -> INFINITE_WATER);
	}
}
