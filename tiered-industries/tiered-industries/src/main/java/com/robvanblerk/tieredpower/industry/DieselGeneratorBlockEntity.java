package com.robvanblerk.tieredpower.industry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Burns liquid fuel, 1 mB a tick: Biodiesel 200 FE/mB, Rocket Fuel 300, Creosote Oil 40. Holds 200,000 FE and pushes
 * power out of every side; idles while full.
 */
public class DieselGeneratorBlockEntity extends BlockEntity {
	public static final int CAPACITY = 200_000, MAX_PUSH = 2_000, TANK = 8_000;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH, this::setChanged);
	public final FluidTank tank = new FluidTank(TANK, f -> fePerMb(f.getFluid()) > 0) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> tank);
	private int generating;

	public DieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.DIESEL_GENERATOR.get(), pos, state);
	}

	public static int fePerMb(Fluid f) {
		if (f == ModFluids.BIODIESEL.get()) return 200;
		if (f == ModFluids.ROCKET_FUEL.get()) return 300;
		if (f == ModFluids.CREOSOTE.get()) return 40;
		return 0;
	}

	public int getGenerating() { return generating; }

	public static void tick(Level level, BlockPos pos, BlockState state, DieselGeneratorBlockEntity be) {
		if (level.isClientSide()) return;
		be.generating = 0;
		int per = be.tank.isEmpty() ? 0 : fePerMb(be.tank.getFluid().getFluid());
		if (per > 0 && be.energy.getMaxEnergyStored() - be.energy.getEnergyStored() >= per) {
			be.tank.drain(1, IFluidHandler.FluidAction.EXECUTE);
			be.generating = com.robvanblerk.tieredpower.Config.gen(per);
			be.energy.addInternal(be.generating);
		}
		for (Direction d : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, d), MAX_PUSH);
		}
		boolean lit = be.generating > 0;
		if (state.hasProperty(DieselGeneratorBlock.LIT) && state.getValue(DieselGeneratorBlock.LIT) != lit && level.getGameTime() % 10 == 0)
			level.setBlock(pos, state.setValue(DieselGeneratorBlock.LIT, lit), Block.UPDATE_CLIENTS);
		be.setChanged();
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); energyCap.invalidate(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); energyCap = LazyOptional.of(() -> energy); fluidCap = LazyOptional.of(() -> tank); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		tag.put("tank", tank.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		tank.readFromNBT(tag.getCompound("tank"));
	}
}
