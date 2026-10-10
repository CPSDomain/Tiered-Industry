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
import net.minecraftforge.energy.IEnergyStorage;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.FusionReactorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * End-game power.
 *
 * 1. Charging: while off, it accepts FE from cables into its ignition capacitor (1,000,000 FE).
 * 2. Ignition: once fully charged AND loaded with a Deuterium + Tritium cell, it fires, using up the charge.
 * 3. Running: each Deuterium + Tritium pair burns for 2,400 ticks (2 minutes). Plasma heats up over ~9 seconds;
 *    output is proportional to plasma temperature, up to 8,000 FE/t (config). Empty cells come back out.
 * 4. Out of fuel: the plasma cools over ~25 seconds. Refuel in time and it keeps running;
 *    if it reaches zero it needs a full re-ignition.
 * If nothing is taking the power (output buffer full), it pauses burning and holds temperature.
 */
public class FusionReactorBlockEntity extends MachineBlockEntity {
	public static final int IGNITION_ENERGY = 1_000_000;
	public static final int CHARGE_RATE = 32_000;
	public static final int OUTPUT_CAPACITY = 2_000_000;
	public static final int MAX_PUSH = 32_000;
	public static final int MAX_OUTPUT = 4_000;           // FE/t at full temperature
	public static final int BURN_TICKS = 2_400;           // per Deuterium + Tritium pair
	public static final int MAX_TEMP = 1_000;
	public static final int HEAT_RATE = 5, COOL_RATE = 2;

	public static final int DEUTERIUM_SLOT = 0, TRITIUM_SLOT = 1, EMPTY_SLOT = 2;

	private int charge;
	private int temperature;
	private int burnTime;
	private int generated;

	/** What cables see: charging input while off, power output while running. */
	private final IEnergyStorage reactorEnergy = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (!canReceive()) return 0;
			int accepted = Math.min(Math.min(maxReceive, CHARGE_RATE), IGNITION_ENERGY - charge);
			if (!simulate && accepted > 0) {
				charge += accepted;
				setChanged();
			}
			return accepted;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return energy.extractEnergy(maxExtract, simulate);
		}

		@Override
		public int getEnergyStored() {
			return energy.getEnergyStored() + charge;
		}

		@Override
		public int getMaxEnergyStored() {
			return OUTPUT_CAPACITY + IGNITION_ENERGY;
		}

		@Override
		public boolean canExtract() {
			return true;
		}

		@Override
		public boolean canReceive() {
			return temperature <= 0 && charge < IGNITION_ENERGY;
		}
	};
	private LazyOptional<IEnergyStorage> reactorCap = LazyOptional.of(() -> reactorEnergy);
	/** Deuterium + tritium gas from Gas Pipes - burned before cells. */
	private final FusionGasTank gas = new FusionGasTank(this::setChanged);
	private LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> gasCap = LazyOptional.of(() -> gas);

	public FusionGasTank gasTank() {
		return gas;
	}

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> charge & 0xFFFF;
				case 3 -> (charge >>> 16) & 0xFFFF;
				case 4 -> temperature;
				case 5 -> burnTime;
				case 6 -> generated;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return FusionReactorMenu.DATA_COUNT;
		}
	};

	public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUSION_REACTOR.get(), pos, state, 3, OUTPUT_CAPACITY, 0, MAX_PUSH);
	}

	// ---- Plasma Coils (plasma tier) ----
	private int plasmaTier;

	public int getTemperature() {
		return temperature;
	}

	public int getGenerated() {
		return generated;
	}

	public int getPlasmaTier() {
		return plasmaTier;
	}

	public void setPlasmaTier(int tier) {
		plasmaTier = PlasmaTiers.clamp(tier);
		setChanged();
	}

	@Override
	public java.util.List<ItemStack> installedDrops() {
		java.util.List<ItemStack> out = new java.util.ArrayList<>();
		for (int t = 1; t <= plasmaTier; t++) out.add(new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.PLASMA_COILS.get(t - 1).get()));
		return out;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, FusionReactorBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		be.generated = 0;

		if (be.temperature <= 0) {
			// Off: ignite once fully charged and fuelled.
			if (be.charge >= IGNITION_ENERGY && be.consumeFuelPair()) {
				be.charge = 0;
				be.burnTime = PlasmaTiers.burnTicks(BURN_TICKS, be.plasmaTier);
				be.temperature = HEAT_RATE;
			}
		} else if (be.energy.getSpace() > 0) {
			// Running and there's somewhere for the power to go.
			if (be.burnTime <= 0 && be.consumeFuelPair()) be.burnTime = PlasmaTiers.burnTicks(BURN_TICKS, be.plasmaTier);

			if (be.burnTime > 0) {
				be.burnTime--;
				be.temperature = Math.min(MAX_TEMP, be.temperature + HEAT_RATE);
				be.generated = PlasmaTiers.output((int) ((long) com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.COMPACT_FUSION_OUTPUT) * be.temperature / MAX_TEMP), be.plasmaTier);
				be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generated));
			} else {
				be.temperature = Math.max(0, be.temperature - COOL_RATE); // out of fuel: cooling
			}
		}
		// (Output full: hold temperature and fuel until the power is used.)

		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
		}

		boolean running = be.temperature > 0;
		if (state.getValue(MachineBlock.LIT) != running) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, running), Block.UPDATE_ALL);
		}
		be.setChanged();
	}

	/** Takes one Deuterium and one Tritium cell, returning two empty cells. */
	private boolean consumeFuelPair() {
		if (gas.consumePair()) return true; // piped gas first
		ItemStack d = items.get(DEUTERIUM_SLOT), t = items.get(TRITIUM_SLOT), empty = items.get(EMPTY_SLOT);
		if (!d.is(ModBlocks.DEUTERIUM_CELL.get()) || !t.is(ModBlocks.TRITIUM_CELL.get())) return false;
		if (!empty.isEmpty() && (!empty.is(ModBlocks.EMPTY_FUEL_CELL.get()) || empty.getCount() + 2 > empty.getMaxStackSize())) return false;
		d.shrink(1);
		t.shrink(1);
		if (empty.isEmpty()) items.set(EMPTY_SLOT, new ItemStack(ModBlocks.EMPTY_FUEL_CELL.get(), 2));
		else empty.grow(2);
		return true;
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.ENERGY) return reactorCap.cast();
		if (cap == ForgeCapabilities.FLUID_HANDLER) return gasCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		gasCap.invalidate();
		reactorCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		reactorCap = LazyOptional.of(() -> reactorEnergy);
		gasCap = LazyOptional.of(() -> gas);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("plasmaTier", plasmaTier);
		gas.save(tag);
		tag.putInt("charge", charge);
		tag.putInt("temperature", temperature);
		tag.putInt("burn_time", burnTime);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		plasmaTier = PlasmaTiers.clamp(tag.getInt("plasmaTier"));
		gas.load(tag);
		charge = tag.getInt("charge");
		temperature = tag.getInt("temperature");
		burnTime = tag.getInt("burn_time");
	}

	// Hoppers/pipes: deuterium from the top, tritium from the sides, empty cells out of the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.DOWN) return new int[]{EMPTY_SLOT};
		if (side == Direction.UP) return new int[]{DEUTERIUM_SLOT};
		return new int[]{TRITIUM_SLOT, DEUTERIUM_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == EMPTY_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == DEUTERIUM_SLOT) return stack.is(ModBlocks.DEUTERIUM_CELL.get());
		if (slot == TRITIUM_SLOT) return stack.is(ModBlocks.TRITIUM_CELL.get());
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.fusion_reactor");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FusionReactorMenu(containerId, inventory, this, data);
	}
}
