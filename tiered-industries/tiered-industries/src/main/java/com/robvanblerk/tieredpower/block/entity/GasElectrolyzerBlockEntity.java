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
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.item.powered.HydrogenJetpackItem;
import com.robvanblerk.tieredpower.menu.GasElectrolyzerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Splits water into hydrogen and oxygen: 10 mB water -> 20 mB hydrogen + 10 mB oxygen per tick (faster with Speed Upgrades),
 * 200 FE/t. Pulls water from neighbours and pushes both gases out into Gas Pipes / tanks / Gas Burners.
 * The slot refuels a Hydrogen Jetpack. Slots: 0 jetpack, 1-2 upgrades.
 */
public class GasElectrolyzerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 100_000, MAX_INPUT = 4_000, ENERGY_PER_TICK = 200;
	public static final int TANK = 8_000, WATER_PER_TICK = 10;

	private int water, hydrogen, oxygen;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
				case 1 -> hydrogen > 0 ? new FluidStack(ModFluids.HYDROGEN.get(), hydrogen) : FluidStack.EMPTY;
				default -> oxygen > 0 ? new FluidStack(ModFluids.OXYGEN.get(), oxygen) : FluidStack.EMPTY;
			};
		}

		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 && s.getFluid().isSame(Fluids.WATER); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (!r.getFluid().isSame(Fluids.WATER)) return 0;
			int n = Math.min(r.getAmount(), TANK - water);
			if (a.execute() && n > 0) { water += n; setChanged(); }
			return n;
		}

		@Override
		public @NotNull FluidStack drain(FluidStack r, FluidAction a) {
			if (r.getFluid() == ModFluids.HYDROGEN.get()) return take(true, r.getAmount(), a);
			if (r.getFluid() == ModFluids.OXYGEN.get()) return take(false, r.getAmount(), a);
			return FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			return hydrogen > 0 ? take(true, max, a) : take(false, max, a);
		}

		private FluidStack take(boolean h, int max, FluidAction a) {
			int n = Math.min(max, h ? hydrogen : oxygen);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) {
				if (h) hydrogen -= n; else oxygen -= n;
				setChanged();
			}
			return new FluidStack(h ? ModFluids.HYDROGEN.get() : ModFluids.OXYGEN.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> water;
				case 3 -> hydrogen;
				case 4 -> oxygen;
				case 5 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return GasElectrolyzerMenu.DATA_COUNT; }
	};

	public GasElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GAS_ELECTROLYZER.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(1);
	}

	public int getHydrogen() { return hydrogen; }
	public int getOxygen() { return oxygen; }

	public static void tick(Level level, BlockPos pos, BlockState state, GasElectrolyzerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (be.water < TANK) be.water += GasTanks.pull(level, pos, Fluids.WATER, Math.min(1_000, TANK - be.water));

		int rate = WATER_PER_TICK * be.progressStep() / 100;
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean working = be.water >= rate && be.hydrogen + rate * 2 <= TANK && be.oxygen + rate <= TANK && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.water -= rate;
			be.hydrogen += rate * 2;
			be.oxygen += rate;
		}

		// Refuel a Hydrogen Jetpack in the slot.
		ItemStack pack = be.items.get(0);
		if (pack.getItem() instanceof HydrogenJetpackItem jet && be.hydrogen > 0) be.hydrogen -= jet.fill(pack, Math.min(be.hydrogen, 200));

		if (be.hydrogen > 0) be.hydrogen = GasTanks.push(level, pos, ModFluids.HYDROGEN.get(), be.hydrogen);
		if (be.oxygen > 0) be.oxygen = GasTanks.push(level, pos, ModFluids.OXYGEN.get(), be.oxygen);
		be.setChanged();
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
		tag.putInt("water", water);
		tag.putInt("hydrogen", hydrogen);
		tag.putInt("oxygen", oxygen);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water = tag.getInt("water");
		hydrogen = tag.getInt("hydrogen");
		oxygen = tag.getInt("oxygen");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == 0 && stack.getItem() instanceof HydrogenJetpackItem jet && jet.fuel(stack) >= jet.getCapacity();
	}
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.getItem() instanceof HydrogenJetpackItem; }
	@Override public int getMaxStackSize() { return 1; }

	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.gas_electrolyzer"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new GasElectrolyzerMenu(id, inv, this, data);
	}
}
