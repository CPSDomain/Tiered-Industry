package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.CondenserBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Turns steam back into water: 10 mB of steam -> 1 mB of water (the reverse of boiling), up to 1,000 mB of steam per tick.
 * Takes steam from Gas Pipes, Boilers or reactors next to it, and pushes the water into anything that accepts it -
 * put it in a loop with a Fission Reactor's Coolant Port and the water goes straight back into the reactor.
 * Needs no power.
 */
public class CondenserBlockEntity extends BlockEntity {
	public static final int STEAM_CAPACITY = 16_000, WATER_CAPACITY = 16_000, STEAM_PER_TICK = 1_000, STEAM_PER_WATER = 10;

	private int steam, water;
	private int condensing; // mB of steam per tick, for Jade

	private final IFluidHandler handler = new IFluidHandler() {
		@Override public int getTanks() { return 2; }

		@Override
		public @NotNull FluidStack getFluidInTank(int tank) {
			if (tank == 0) return steam > 0 ? new FluidStack(ModFluids.STEAM.get(), steam) : FluidStack.EMPTY;
			return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
		}

		@Override public int getTankCapacity(int tank) { return tank == 0 ? STEAM_CAPACITY : WATER_CAPACITY; }
		@Override public boolean isFluidValid(int tank, @NotNull FluidStack s) { return tank == 0 && s.getFluid() == ModFluids.STEAM.get(); }

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			if (resource.getFluid() != ModFluids.STEAM.get()) return 0;
			int accepted = Math.min(resource.getAmount(), STEAM_CAPACITY - steam);
			if (action.execute() && accepted > 0) {
				steam += accepted;
				setChanged();
			}
			return accepted;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
			return resource.getFluid() == Fluids.WATER ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
			int amount = Math.min(maxDrain, water);
			if (amount <= 0) return FluidStack.EMPTY;
			if (action.execute()) {
				water -= amount;
				setChanged();
			}
			return new FluidStack(Fluids.WATER, amount);
		}
	};
	private LazyOptional<IFluidHandler> cap = LazyOptional.of(() -> handler);

	public CondenserBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CONDENSER.get(), pos, state);
	}

	public int getCondensing() { return condensing; }
	public int getSteam() { return steam; }
	public int getWater() { return water; }

	public static void tick(Level level, BlockPos pos, BlockState state, CondenserBlockEntity be) {
		// Condense.
		int steamUse = Math.min(Math.min(be.steam, STEAM_PER_TICK), (WATER_CAPACITY - be.water) * STEAM_PER_WATER);
		steamUse -= steamUse % STEAM_PER_WATER;
		be.condensing = steamUse;
		if (steamUse > 0) {
			be.steam -= steamUse;
			be.water += steamUse / STEAM_PER_WATER;
			be.setChanged();
		}
		// Push water out to anything that takes it.
		if (be.water > 0) {
			for (Direction dir : Direction.values()) {
				if (be.water <= 0) break;
				BlockPos next = pos.relative(dir);
				if (!level.isLoaded(next)) continue;
				BlockEntity n = level.getBlockEntity(next);
				if (n == null || n instanceof CondenserBlockEntity) continue;
				IFluidHandler target = n.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (target == null) continue;
				int given = target.fill(new FluidStack(Fluids.WATER, be.water), IFluidHandler.FluidAction.EXECUTE);
				be.water -= given;
			}
		}
		boolean active = steamUse > 0;
		if (state.getValue(CondenserBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(CondenserBlock.ACTIVE, active), Block.UPDATE_ALL);
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
		cap = LazyOptional.of(() -> handler);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("steam", steam);
		tag.putInt("water", water);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		steam = tag.getInt("steam");
		water = tag.getInt("water");
	}
}
