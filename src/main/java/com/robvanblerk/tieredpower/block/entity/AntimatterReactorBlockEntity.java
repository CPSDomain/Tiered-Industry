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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.AntimatterReactorBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * The end-game generator: annihilates antimatter for 50,000 FE per mB, burning up to 20 mB per tick (1,000,000 FE/t).
 * Takes antimatter from Gas Pipes or a Particle Collider next to it; holds 50,000,000 FE and pushes power out of every
 * side. Idles (burning nothing) while its buffer is full.
 */
public class AntimatterReactorBlockEntity extends BlockEntity {
	public static final int CAPACITY = 50_000_000, MAX_PUSH = 1_000_000, FE_PER_MB = 50_000, BURN_PER_TICK = 20, TANK = 16_000;
	public final ModEnergyStorage energy = new ModEnergyStorage(CAPACITY, 0, MAX_PUSH, this::setChanged);
	private LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energy);
	private int antimatter, generating;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return antimatter > 0 ? new FluidStack(ModFluids.ANTIMATTER.get(), antimatter) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.ANTIMATTER.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.ANTIMATTER.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - antimatter);
			if (a.execute() && n > 0) { antimatter += n; setChanged(); }
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	public AntimatterReactorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ANTIMATTER_REACTOR.get(), pos, state);
	}

	public int getAntimatter() { return antimatter; }
	public int getGenerating() { return generating; }

	public static void tick(Level level, BlockPos pos, BlockState state, AntimatterReactorBlockEntity be) {
		int space = be.energy.getMaxEnergyStored() - be.energy.getEnergyStored();
		int perMb = com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.ANTIMATTER_FE_PER_MB);
		int burn = Math.min(BURN_PER_TICK, Math.min(be.antimatter, space / Math.max(1, perMb)));
		be.generating = 0;
		if (burn > 0) {
			be.antimatter -= burn;
			be.generating = burn * com.robvanblerk.tieredpower.Config.gen(perMb);
			be.energy.addInternal(be.generating);
			be.setChanged();
		}
		for (Direction d : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, d), MAX_PUSH);
		}
		boolean lit = be.generating > 0;
		if (state.hasProperty(AntimatterReactorBlock.LIT) && state.getValue(AntimatterReactorBlock.LIT) != lit && level.getGameTime() % 10 == 0)
			level.setBlock(pos, state.setValue(AntimatterReactorBlock.LIT, lit), Block.UPDATE_CLIENTS);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energyCap.invalidate();
		fluidCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		energyCap = LazyOptional.of(() -> energy);
		fluidCap = LazyOptional.of(() -> fluids);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
		tag.putInt("antimatter", antimatter);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		antimatter = tag.getInt("antimatter");
	}
}
