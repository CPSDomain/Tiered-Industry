package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Deuterium and tritium gas for a Fusion Reactor, piped in through Gas Pipes. One fuel pair (the same as one Deuterium
 * Cell plus one Tritium Cell) is 250 mB of each. Fill-only: nothing can be drained back out.
 */
public class FusionGasTank implements IFluidHandler {
	public static final int CAPACITY = 8_000, PER_PAIR = 250;
	private final Runnable onChanged;
	private int deuterium, tritium;

	public FusionGasTank(Runnable onChanged) {
		this.onChanged = onChanged;
	}

	public int deuterium() { return deuterium; }
	public int tritium() { return tritium; }

	/** Uses one pair's worth of gas if there's enough of both. */
	public boolean consumePair() {
		if (deuterium < PER_PAIR || tritium < PER_PAIR) return false;
		deuterium -= PER_PAIR;
		tritium -= PER_PAIR;
		onChanged.run();
		return true;
	}

	private boolean isDeuterium(FluidStack s) { return s.getFluid() == ModFluids.DEUTERIUM.get(); }
	private boolean isTritium(FluidStack s) { return s.getFluid() == ModFluids.TRITIUM.get(); }

	@Override public int getTanks() { return 2; }

	@Override
	public @NotNull FluidStack getFluidInTank(int tank) {
		if (tank == 0) return deuterium > 0 ? new FluidStack(ModFluids.DEUTERIUM.get(), deuterium) : FluidStack.EMPTY;
		return tritium > 0 ? new FluidStack(ModFluids.TRITIUM.get(), tritium) : FluidStack.EMPTY;
	}

	@Override public int getTankCapacity(int tank) { return CAPACITY; }
	@Override public boolean isFluidValid(int tank, @NotNull FluidStack stack) { return tank == 0 ? isDeuterium(stack) : isTritium(stack); }

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		boolean d = isDeuterium(resource);
		if (!d && !isTritium(resource)) return 0;
		int n = Math.min(resource.getAmount(), CAPACITY - (d ? deuterium : tritium));
		if (n > 0 && action.execute()) {
			if (d) deuterium += n; else tritium += n;
			onChanged.run();
		}
		return Math.max(0, n);
	}

	@Override public @NotNull FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
	@Override public @NotNull FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }

	public void save(CompoundTag tag) {
		tag.putInt("gasDeuterium", deuterium);
		tag.putInt("gasTritium", tritium);
	}

	public void load(CompoundTag tag) {
		deuterium = tag.getInt("gasDeuterium");
		tritium = tag.getInt("gasTritium");
	}
}
