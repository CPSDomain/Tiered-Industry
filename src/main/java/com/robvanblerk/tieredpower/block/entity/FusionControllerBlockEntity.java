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
import com.robvanblerk.tieredpower.menu.FusionControllerMenu;
import com.robvanblerk.tieredpower.multiblock.FusionStructure;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/**
 * Multiblock Fusion Reactor controller. Works like the compact reactor (charge -> ignite -> burn Deuterium + Tritium
 * pairs), but output scales with the number of Magnet Coils inside the structure. Items go in and out through
 * Fuel Ports; charge comes in and power goes out through Power Ports.
 */
public class FusionControllerBlockEntity extends MachineBlockEntity {
	public static final int IGNITION_ENERGY = 5_000_000;
	public static final int CHARGE_RATE = 128_000;
	public static final int OUTPUT_CAPACITY = 20_000_000;
	public static final int MAX_PUSH = 512_000; // high enough for a Mk III reactor
	public static final int BASE_OUTPUT = 4_000;
	public static final int OUTPUT_PER_COIL = 1_500;
	public static final int BURN_TICKS = 2_400;
	/** Reactors with more coils than this burn fuel proportionally faster, so FE per fuel pair stays balanced. */
	public static final int FULL_BURN_COILS = 27;
	public static final int MAX_TEMP = 1_000;
	public static final int HEAT_RATE = 5, COOL_RATE = 2;
	public static final int CHECK_INTERVAL = 20;

	public static final int DEUTERIUM_SLOT = 0, TRITIUM_SLOT = 1, EMPTY_SLOT = 2;

	private boolean formed;
	private java.util.List<BlockPos> coilPositions = java.util.List.of();
	private boolean coilsLit;
	private int coils;
	private int checkTimer = CHECK_INTERVAL; // check on the first tick

	private int charge;
	private int temperature;
	private int burnTime;
	private int generated;

	/** Charging input while off, power output while running. Shared by the controller and every Power Port. */
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
			return formed ? energy.extractEnergy(maxExtract, simulate) : 0;
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
			return formed;
		}

		@Override
		public boolean canReceive() {
			return formed && temperature <= 0 && charge < IGNITION_ENERGY;
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
				case 6 -> generated & 0xFFFF;
				case 9 -> (generated >>> 16) & 0xFFFF;
				case 7 -> formed ? 1 : 0;
				case 8 -> coils;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return FusionControllerMenu.DATA_COUNT;
		}
	};

	public FusionControllerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FUSION_CONTROLLER.get(), pos, state, 3, OUTPUT_CAPACITY, 0, MAX_PUSH);
	}

	public int getCoils() {
		return coils;
	}

	public int getGenerated() {
		return generated;
	}

	public int getTemperature() {
		return temperature;
	}

	public boolean isFormed() {
		return formed;
	}

	public IEnergyStorage getReactorEnergy() {
		return reactorEnergy;
	}

	/** Ticks one Deuterium + Tritium pair lasts: 2 minutes up to 27 coils, shorter for bigger reactors. */
	public int getBurnTicks() {
		int base = coils <= FULL_BURN_COILS ? BURN_TICKS : Math.max(100, BURN_TICKS * FULL_BURN_COILS / coils);
		return PlasmaTiers.burnTicks(base, plasmaTier);
	}

	public int getMaxOutput() {
		return PlasmaTiers.output(maxOutputFor(coils), plasmaTier);
	}

	/** Output for a reactor with this many coils, using the config values (defaults 4,000 + 1,500 per coil). */
	public static int maxOutputFor(int coils) {
		long out = (long) com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.FUSION_BASE_OUTPUT)
				+ (long) com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.FUSION_OUTPUT_PER_COIL) * coils;
		return (int) Math.min(Integer.MAX_VALUE, out);
	}

	/** Re-checks the structure now, links the ports, and returns a message describing the result. */
	public Component revalidate() {
		checkTimer = 0;
		if (level == null) return Component.empty();
		FusionStructure.Result result = FusionStructure.check(level, worldPosition, getBlockState().getValue(MachineBlock.FACING));
		boolean wasFormed = formed;
		formed = result.formed();
		coils = result.coils();
		if (formed) {
			if (!coilPositions.equals(result.coilPositions())) {
				setCoilsLit(false);
				coilPositions = result.coilPositions();
			}
		} else if (wasFormed) {
			setCoilsLit(false);
		}
		if (formed) {
			for (BlockPos portPos : result.ports()) {
				if (level.getBlockEntity(portPos) instanceof ReactorPortBlockEntity port) port.setController(worldPosition);
				if (level.getBlockEntity(portPos) instanceof ReactorGaugeBlockEntity gauge) gauge.setController(worldPosition);
			}
		}
		setChanged();
		return result.message();
	}

	// ---- Plasma Coils (plasma tier) ----
	private int plasmaTier;

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

	public static void tick(Level level, BlockPos pos, BlockState state, FusionControllerBlockEntity be) {
		if (++be.checkTimer >= CHECK_INTERVAL) be.revalidate();
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		be.generated = 0;

		if (!be.formed) {
			// Broken structure: plasma collapses quickly.
			be.temperature = Math.max(0, be.temperature - COOL_RATE * 10);
		} else if (be.temperature <= 0) {
			if (be.charge >= IGNITION_ENERGY && be.consumeFuelPair()) {
				be.charge = 0;
				be.burnTime = be.getBurnTicks();
				be.temperature = HEAT_RATE;
			}
		} else if (be.energy.getSpace() > 0) {
			if (be.burnTime <= 0 && be.consumeFuelPair()) be.burnTime = be.getBurnTicks();
			if (be.burnTime > 0) {
				be.burnTime--;
				be.temperature = Math.min(MAX_TEMP, be.temperature + HEAT_RATE);
				be.generated = (int) ((long) be.getMaxOutput() * be.temperature / MAX_TEMP);
				be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(be.generated));
			} else {
				be.temperature = Math.max(0, be.temperature - COOL_RATE);
			}
		}

		boolean running = be.temperature > 0;
		if (be.formed && running != be.coilsLit) be.setCoilsLit(running);
		if (state.getValue(MachineBlock.LIT) != running) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, running), Block.UPDATE_ALL);
		}
		be.setChanged();
	}

	/** Switches the Magnet Coils' animated "active" look on or off. */
	private void setCoilsLit(boolean lit) {
		if (level == null) return;
		for (BlockPos p : coilPositions) {
			if (!level.isLoaded(p)) continue;
			BlockState s = level.getBlockState(p);
			if (s.getBlock() instanceof com.robvanblerk.tieredpower.block.MagnetCoilBlock && s.getValue(com.robvanblerk.tieredpower.block.MagnetCoilBlock.ACTIVE) != lit) {
				level.setBlock(p, s.setValue(com.robvanblerk.tieredpower.block.MagnetCoilBlock.ACTIVE, lit), Block.UPDATE_CLIENTS);
			}
		}
		coilsLit = lit;
	}

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

	// Fuel Ports (and hoppers on the controller) can insert cells and pull out empties from any side.
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{DEUTERIUM_SLOT, TRITIUM_SLOT, EMPTY_SLOT};
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
		return Component.translatable("block.tieredpower.fusion_controller");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FusionControllerMenu(containerId, inventory, this, data);
	}
}
