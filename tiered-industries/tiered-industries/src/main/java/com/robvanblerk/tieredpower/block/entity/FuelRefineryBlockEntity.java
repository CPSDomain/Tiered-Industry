package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.RocketFuelMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Blends methalox Rocket Fuel: 10 mB Liquid Methane + 20 mB Liquid Oxygen + 100 FE = 30 mB Rocket Fuel per operation
 * (one a tick, more with Speed Upgrades). Rocket Fuel comes out into Fluid Pipes. Slots: 0-1 upgrades.
 */
public class FuelRefineryBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 200_000, MAX_INPUT = 8_000, ENERGY_PER_OP = 100, TANK = 16_000;
	public static final int METHANE_PER_OP = 10, OXYGEN_PER_OP = 20, FUEL_PER_OP = 30;
	private int methane, oxygen, fuel;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> methane > 0 ? new FluidStack(ModFluids.LIQUID_METHANE.get(), methane) : FluidStack.EMPTY;
				case 1 -> oxygen > 0 ? new FluidStack(ModFluids.LIQUID_OXYGEN.get(), oxygen) : FluidStack.EMPTY;
				default -> fuel > 0 ? new FluidStack(ModFluids.ROCKET_FUEL.get(), fuel) : FluidStack.EMPTY;
			};
		}

		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? s.getFluid() == ModFluids.LIQUID_METHANE.get() : t == 1 && s.getFluid() == ModFluids.LIQUID_OXYGEN.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			boolean m = r.getFluid() == ModFluids.LIQUID_METHANE.get();
			if (!m && r.getFluid() != ModFluids.LIQUID_OXYGEN.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - (m ? methane : oxygen));
			if (a.execute() && n > 0) {
				if (m) methane += n; else oxygen += n;
				setChanged();
			}
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return r.getFluid() == ModFluids.ROCKET_FUEL.get() ? drain(r.getAmount(), a) : FluidStack.EMPTY; }

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, fuel);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) { fuel -= n; setChanged(); }
			return new FluidStack(ModFluids.ROCKET_FUEL.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> methane;
				case 3 -> oxygen;
				case 4 -> fuel;
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return RocketFuelMenu.DATA_COUNT; }
	};

	public FuelRefineryBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUEL_REFINERY.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FuelRefineryBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		int cost = be.energyCost(ENERGY_PER_OP);
		int ops = Math.max(1, be.progressStep() / 100);
		ops = Math.min(ops, be.methane / METHANE_PER_OP);
		ops = Math.min(ops, be.oxygen / OXYGEN_PER_OP);
		ops = Math.min(ops, (TANK - be.fuel) / FUEL_PER_OP);
		ops = Math.min(ops, be.energy.getEnergyStored() / Math.max(1, cost));
		if (ops > 0) {
			be.energy.removeInternal(cost * ops);
			be.methane -= METHANE_PER_OP * ops;
			be.oxygen -= OXYGEN_PER_OP * ops;
			be.fuel += FUEL_PER_OP * ops;
		}
		if (be.fuel > 0) be.fuel = GasTanks.push(level, pos, ModFluids.ROCKET_FUEL.get(), be.fuel);
		be.setChanged();
		boolean working = ops > 0;
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> fluids); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("methane", methane);
		tag.putInt("oxygen", oxygen);
		tag.putInt("fuel", fuel);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		methane = tag.getInt("methane");
		oxygen = tag.getInt("oxygen");
		fuel = tag.getInt("fuel");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.fuel_refinery"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new RocketFuelMenu(com.robvanblerk.tieredpower.registry.ModMenus.FUEL_REFINERY.get(), id, inv, this, data, true);
	}
}
