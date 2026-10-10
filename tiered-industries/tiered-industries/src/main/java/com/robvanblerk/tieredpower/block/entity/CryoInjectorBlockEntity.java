package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.CryoInjectorBlock;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Cools a Fission Reactor (single-block, or a multiblock's Fission Controller) with nitrogen so it can run harder:
 * while it feeds 20 mB of nitrogen per tick, the reactor makes 50% more power. Pipe nitrogen in from an Air Separator.
 */
public class CryoInjectorBlockEntity extends BlockEntity {
	public static final int TANK = 8_000, PER_TICK = 20;
	private int nitrogen;
	private boolean active;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return nitrogen > 0 ? new FluidStack(ModFluids.NITROGEN.get(), nitrogen) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.NITROGEN.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.NITROGEN.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - nitrogen);
			if (a.execute() && n > 0) { nitrogen += n; setChanged(); }
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	public CryoInjectorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CRYO_INJECTOR.get(), pos, state);
	}

	public boolean isActive() { return active; }
	public int getNitrogen() { return nitrogen; }

	public static void tick(Level level, BlockPos pos, BlockState state, CryoInjectorBlockEntity be) {
		boolean now = false;
		if (be.nitrogen >= PER_TICK) {
			for (Direction d : Direction.values()) {
				BlockEntity n = level.getBlockEntity(pos.relative(d));
				boolean boosted = n instanceof FissionReactorBlockEntity r ? r.applyCryoBoost()
						: n instanceof FissionControllerBlockEntity c && c.applyCryoBoost();
				if (boosted) {
					be.nitrogen -= PER_TICK;
					be.setChanged();
					now = true;
					break;
				}
			}
		}
		be.active = now;
		if (level.getGameTime() % 10 == 0 && state.getValue(CryoInjectorBlock.LIT) != now)
			level.setBlock(pos, state.setValue(CryoInjectorBlock.LIT, now), 3);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> fluids); }

	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("nitrogen", nitrogen); }
	@Override public void load(CompoundTag tag) { super.load(tag); nitrogen = tag.getInt("nitrogen"); }
}
