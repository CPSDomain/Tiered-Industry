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
import com.robvanblerk.tieredpower.menu.BrineElectrolyzerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModTags;

/**
 * Electrolyses salt water: 1 salt + 500 mB water = 250 mB chlorine + 250 mB hydrogen, in 2 seconds at 200 FE/t.
 * Pushes both gases into Gas Pipes. Slots: 0 salt, 1-2 upgrades.
 */
public class BrineElectrolyzerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 80_000, MAX_INPUT = 4_000, ENERGY_PER_TICK = 200, TICKS = 40, TANK = 8_000;
	public static final int WATER_PER_OP = 500, GAS_PER_OP = 250, INPUT_SLOT = 0;
	private int water, chlorine, hydrogen, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }

		@Override
		public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
				case 1 -> chlorine > 0 ? new FluidStack(ModFluids.CHLORINE.get(), chlorine) : FluidStack.EMPTY;
				default -> hydrogen > 0 ? new FluidStack(ModFluids.HYDROGEN.get(), hydrogen) : FluidStack.EMPTY;
			};
		}

		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 && s.getFluid().isSame(Fluids.WATER); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (!r.getFluid().isSame(Fluids.WATER)) return 0;
			int n = Math.min(r.getAmount(), TANK - water);
			if (a.execute() && n > 0) { water += n; setChanged(); }
			return Math.max(0, n);
		}

		@Override
		public @NotNull FluidStack drain(FluidStack r, FluidAction a) {
			if (r.getFluid() == ModFluids.CHLORINE.get()) return take(true, r.getAmount(), a);
			if (r.getFluid() == ModFluids.HYDROGEN.get()) return take(false, r.getAmount(), a);
			return FluidStack.EMPTY;
		}

		@Override public @NotNull FluidStack drain(int max, FluidAction a) { return chlorine > 0 ? take(true, max, a) : take(false, max, a); }

		private FluidStack take(boolean cl, int max, FluidAction a) {
			int n = Math.min(max, cl ? chlorine : hydrogen);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) {
				if (cl) chlorine -= n; else hydrogen -= n;
				setChanged();
			}
			return new FluidStack(cl ? ModFluids.CHLORINE.get() : ModFluids.HYDROGEN.get(), n);
		}
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> water;
				case 5 -> chlorine;
				case 6 -> hydrogen;
				case 7 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return BrineElectrolyzerMenu.DATA_COUNT; }
	};

	public BrineElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BRINE_ELECTROLYZER.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(1);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BrineElectrolyzerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (be.water < TANK) be.water += GasTanks.pull(level, pos, Fluids.WATER, Math.min(1_000, TANK - be.water));
		ItemStack salt = be.items.get(INPUT_SLOT);
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean working = salt.is(ModTags.SALT) && be.water >= WATER_PER_OP && be.chlorine + GAS_PER_OP <= TANK && be.hydrogen + GAS_PER_OP <= TANK
				&& be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				salt.shrink(1);
				be.water -= WATER_PER_OP;
				be.chlorine += GAS_PER_OP;
				be.hydrogen += GAS_PER_OP;
			}
		} else if (!salt.is(ModTags.SALT)) {
			be.progress = 0;
		}
		if (be.chlorine > 0) be.chlorine = GasTanks.push(level, pos, ModFluids.CHLORINE.get(), be.chlorine);
		if (be.hydrogen > 0) be.hydrogen = GasTanks.push(level, pos, ModFluids.HYDROGEN.get(), be.hydrogen);
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
		tag.putInt("chlorine", chlorine);
		tag.putInt("hydrogen", hydrogen);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water = tag.getInt("water");
		chlorine = tag.getInt("chlorine");
		hydrogen = tag.getInt("hydrogen");
		progress = tag.getInt("progress");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT && stack.is(ModTags.SALT); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.brine_electrolyzer"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new BrineElectrolyzerMenu(id, inv, this, data);
	}
}
