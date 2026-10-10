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

import com.robvanblerk.tieredpower.Config;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.GasBurnerGeneratorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;

/**
 * Burns gas for power, up to 40 mB/t. Hydrogen: 7 FE per mB (9 with half as much oxygen). Methane (biogas from a
 * Bio-Digester): 12 FE per mB (15 with the same amount of oxygen). Hydrogen is used first. Idles when its buffer is full.
 * Takes gases from Gas Pipes, tanks or a gas machine next to it.
 */
public class GasBurnerGeneratorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 200_000, MAX_PUSH = 2_000, TANK = 8_000;
	public static final int HYDROGEN_PER_TICK = 40, FE_PER_MB = 7, FE_PER_MB_WITH_OXYGEN = 9;
	public static final int METHANE_FE_PER_MB = 12, METHANE_FE_PER_MB_WITH_OXYGEN = 15;

	private int hydrogen, oxygen, methane, generating;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 3; }
		@Override public @NotNull FluidStack getFluidInTank(int t) {
			return switch (t) {
				case 0 -> hydrogen > 0 ? new FluidStack(ModFluids.HYDROGEN.get(), hydrogen) : FluidStack.EMPTY;
				case 1 -> oxygen > 0 ? new FluidStack(ModFluids.OXYGEN.get(), oxygen) : FluidStack.EMPTY;
				default -> methane > 0 ? new FluidStack(ModFluids.METHANE.get(), methane) : FluidStack.EMPTY;
			};
		}
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) {
			return s.getFluid() == (t == 0 ? ModFluids.HYDROGEN.get() : t == 1 ? ModFluids.OXYGEN.get() : ModFluids.METHANE.get());
		}

		@Override
		public int fill(FluidStack r, FluidAction a) {
			int which = r.getFluid() == ModFluids.HYDROGEN.get() ? 0 : r.getFluid() == ModFluids.OXYGEN.get() ? 1 : r.getFluid() == ModFluids.METHANE.get() ? 2 : -1;
			if (which < 0) return 0;
			int have = which == 0 ? hydrogen : which == 1 ? oxygen : methane;
			int n = Math.min(r.getAmount(), TANK - have);
			if (a.execute() && n > 0) {
				if (which == 0) hydrogen += n; else if (which == 1) oxygen += n; else methane += n;
				setChanged();
			}
			return Math.max(0, n);
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> hydrogen;
				case 3 -> oxygen;
				case 4 -> generating;
				case 5 -> methane;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return GasBurnerGeneratorMenu.DATA_COUNT; }
	};

	public GasBurnerGeneratorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.GAS_BURNER_GENERATOR.get(), pos, state, 0, CAPACITY, 0, MAX_PUSH);
	}

	public int getGenerating() { return generating; }

	public static void tick(Level level, BlockPos pos, BlockState state, GasBurnerGeneratorBlockEntity be) {
		boolean paused = be.preTick(level, pos, state);
		be.generating = 0;
		if (!paused && be.hydrogen > 0) {
			int burn = Math.min(be.hydrogen, HYDROGEN_PER_TICK);
			int oxygenNeeded = burn / 2;
			boolean boosted = be.oxygen >= oxygenNeeded && oxygenNeeded > 0;
			int fe = Config.gen(burn * (boosted ? FE_PER_MB_WITH_OXYGEN : FE_PER_MB));
			if (be.energy.getSpace() >= fe) { // idle while full
				be.hydrogen -= burn;
				if (boosted) be.oxygen -= oxygenNeeded;
				be.energy.addInternal(fe);
				be.generating = fe;
			}
		} else if (!paused && be.methane > 0) {
			int burn = Math.min(be.methane, HYDROGEN_PER_TICK);
			boolean boosted = be.oxygen >= burn;
			int fe = Config.gen(burn * (boosted ? METHANE_FE_PER_MB_WITH_OXYGEN : METHANE_FE_PER_MB));
			if (be.energy.getSpace() >= fe) {
				be.methane -= burn;
				if (boosted) be.oxygen -= burn;
				be.energy.addInternal(fe);
				be.generating = fe;
			}
		}
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
		}
		be.setChanged();
		boolean lit = be.generating > 0;
		BlockState now = level.getBlockState(pos);
		if (now.hasProperty(MachineBlock.LIT) && now.getValue(MachineBlock.LIT) != lit) level.setBlock(pos, now.setValue(MachineBlock.LIT, lit), Block.UPDATE_ALL);
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
		tag.putInt("hydrogen", hydrogen);
		tag.putInt("oxygen", oxygen);
		tag.putInt("methane", methane);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		hydrogen = tag.getInt("hydrogen");
		oxygen = tag.getInt("oxygen");
		methane = tag.getInt("methane");
	}

	@Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.gas_burner_generator"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new GasBurnerGeneratorMenu(id, inv, data);
	}
}
