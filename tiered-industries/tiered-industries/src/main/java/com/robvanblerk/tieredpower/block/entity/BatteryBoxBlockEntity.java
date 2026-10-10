package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.BatteryBoxBlock;
import com.robvanblerk.tieredpower.energy.BatteryTier;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.energy.ModEnergyStorage;
import com.robvanblerk.tieredpower.energy.SideMode;
import com.robvanblerk.tieredpower.energy.SidedEnergyView;
import com.robvanblerk.tieredpower.menu.BatteryBoxMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Stores energy. Each face is Input (accepts power), Output (pushes power out) or Disabled (no connection),
 * set in the block state. Tracks the in/out rate for the GUI.
 */
public class BatteryBoxBlockEntity extends BlockEntity implements MenuProvider {
	private final BatteryTier tier;
	public final ModEnergyStorage energy;

	private LazyOptional<IEnergyStorage> inputCapLive, outputCapLive, fullCapLive;

	private int lastEnergy;
	private int rateIn, rateOut;
	private int lastSignal = -1;

	/** 0 when empty, 1-15 in proportion to how full it is. */
	public int getComparatorSignal() {
		int stored = energy.getEnergyStored();
		if (stored <= 0) return 0;
		return 1 + (int) ((long) stored * 14 / Math.max(1, energy.getMaxEnergyStored()));
	}

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> tier.getCapacity() & 0xFFFF;
				case 3 -> (tier.getCapacity() >>> 16) & 0xFFFF;
				case 4 -> rateIn & 0xFFFF;
				case 5 -> (rateIn >>> 16) & 0xFFFF;
				case 6 -> rateOut & 0xFFFF;
				case 7 -> (rateOut >>> 16) & 0xFFFF;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return BatteryBoxMenu.DATA_COUNT;
		}
	};

	public BatteryBoxBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BATTERY_BOX.get(), pos, state);
		this.tier = state.getBlock() instanceof BatteryBoxBlock b ? b.getTier() : BatteryTier.BASIC;
		this.energy = new ModEnergyStorage(tier.getCapacity(), tier.getRate(), tier.getRate(), this::setChanged);
		makeCaps();
	}

	private void makeCaps() {
		inputCapLive = LazyOptional.of(() -> new SidedEnergyView(energy, true, false));
		outputCapLive = LazyOptional.of(() -> new SidedEnergyView(energy, false, true));
		fullCapLive = LazyOptional.of(() -> energy);
	}

	public int getRateIn() {
		return rateIn;
	}

	public int getRateOut() {
		return rateOut;
	}

	public BatteryTier getTier() {
		return tier;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BatteryBoxBlockEntity be) {
		// Whatever arrived since last tick was pushed in by neighbours.
		be.rateIn = Math.max(0, be.energy.getEnergyStored() - be.lastEnergy);

		int out = 0;
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			if (BatteryBoxBlock.getMode(state, dir) != SideMode.OUTPUT) continue;
			out += EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), be.tier.getRate());
		}
		be.rateOut = out;
		be.lastEnergy = be.energy.getEnergyStored();

		int signal = be.getComparatorSignal();
		if (signal != be.lastSignal) {
			be.lastSignal = signal;
			level.updateNeighbourForOutputSignal(pos, state.getBlock());
		}
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) {
			if (side == null) return fullCapLive.cast();
			return switch (BatteryBoxBlock.getMode(getBlockState(), side)) {
				case INPUT -> inputCapLive.cast();
				case OUTPUT -> outputCapLive.cast();
				case DISABLED -> LazyOptional.empty();
			};
		}
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		inputCapLive.invalidate();
		outputCapLive.invalidate();
		fullCapLive.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		makeCaps();
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("energy", energy.getEnergyStored());
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		energy.setEnergy(tag.getInt("energy"));
		lastEnergy = energy.getEnergyStored();
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new BatteryBoxMenu(containerId, inventory, data, ContainerLevelAccess.create(level, worldPosition));
	}
}
