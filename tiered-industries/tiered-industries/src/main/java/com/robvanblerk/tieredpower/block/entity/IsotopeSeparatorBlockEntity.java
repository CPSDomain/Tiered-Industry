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
import com.robvanblerk.tieredpower.menu.IsotopeSeparatorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Pulls deuterium (heavy hydrogen) out of water for fusion: 50 mB of water becomes 1 mB of deuterium gas each tick
 * (faster with Speed Upgrades), using 400 FE/t. Takes water from pipes, pumps or a Sink; pushes deuterium into Gas Pipes
 * - straight into a Fusion Reactor if it's next to one. Slots: 0-1 upgrades.
 */
public class IsotopeSeparatorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 200_000, MAX_INPUT = 8_000, ENERGY_PER_TICK = 200, MB_PER_OP = 2;
	public static final int TANK = 16_000, WATER_PER_MB = 50;

	private int water, deuterium;
	private boolean working;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 2; }
		@Override public @NotNull FluidStack getFluidInTank(int t) {
			if (t == 0) return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
			return deuterium > 0 ? new FluidStack(ModFluids.DEUTERIUM.get(), deuterium) : FluidStack.EMPTY;
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
			return r.getFluid() == ModFluids.DEUTERIUM.get() ? drain(r.getAmount(), a) : FluidStack.EMPTY;
		}

		@Override
		public @NotNull FluidStack drain(int max, FluidAction a) {
			int n = Math.min(max, deuterium);
			if (n <= 0) return FluidStack.EMPTY;
			if (a.execute()) { deuterium -= n; setChanged(); }
			return new FluidStack(ModFluids.DEUTERIUM.get(), n);
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
				case 3 -> deuterium;
				case 4 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return IsotopeSeparatorMenu.DATA_COUNT; }
	};

	public IsotopeSeparatorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ISOTOPE_SEPARATOR.get(), pos, state, 2, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(0);
	}

	private int carry;

	public int getDeuterium() { return deuterium; }

	public static void tick(Level level, BlockPos pos, BlockState state, IsotopeSeparatorBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (be.water < TANK) be.water += GasTanks.pull(level, pos, Fluids.WATER, Math.min(1_000, TANK - be.water));
		int progress = be.carry + be.progressStep();
		int make = progress / 100 * MB_PER_OP;
		be.carry = progress % 100;
		int cost = be.energyCost(ENERGY_PER_TICK);
		be.working = be.water >= make * WATER_PER_MB && be.deuterium + make <= TANK && be.energy.getEnergyStored() >= cost;
		if (be.working) {
			be.energy.removeInternal(cost);
			be.water -= make * WATER_PER_MB;
			be.deuterium += make;
		}
		if (be.deuterium > 0) be.deuterium = GasTanks.push(level, pos, ModFluids.DEUTERIUM.get(), be.deuterium);
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != be.working) level.setBlock(pos, state.setValue(MachineBlock.LIT, be.working), Block.UPDATE_ALL);
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
		tag.putInt("deuterium", deuterium);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water = tag.getInt("water");
		deuterium = tag.getInt("deuterium");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.isotope_separator"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new IsotopeSeparatorMenu(id, inv, this, data);
	}
}
