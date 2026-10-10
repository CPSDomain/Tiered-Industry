package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.ElectrolyzerMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModTags;

/**
 * Makes fusion fuel using power:
 *  - Deuterium Cell: Empty Fuel Cell + 1,000 mB water + 20,000 FE
 *  - Tritium Cell:   Empty Fuel Cell + Lithium Ingot  + 40,000 FE
 * If there's lithium in the lithium slot it makes Tritium, otherwise Deuterium.
 */
public class ElectrolyzerBlockEntity extends MachineBlockEntity {
	public static final int ENERGY_CAPACITY = 200_000;
	public static final int MAX_INPUT = 8_000;
	public static final int ENERGY_PER_TICK = 100;
	public static final int WATER_CAPACITY = 8_000;
	public static final int WATER_PER_DEUTERIUM = 1_000;
	public static final int TICKS_DEUTERIUM = 200;   // 200 x 100 FE = 20,000 FE
	public static final int TICKS_TRITIUM = 400;     // 400 x 100 FE = 40,000 FE
	public static final int PULL_WATER_PER_TICK = 100;

	public static final int CELL_SLOT = 0, LITHIUM_SLOT = 1, OUTPUT_SLOT = 2;

	private final FluidTank water = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid().is(FluidTags.WATER)) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);

	private int progress;
	private int maxProgress = TICKS_DEUTERIUM;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> maxProgress;
				case 4 -> water.getFluidAmount();
				case 5 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return ElectrolyzerMenu.DATA_COUNT;
		}
	};

	public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ELECTROLYZER.get(), pos, state, 5, ENERGY_CAPACITY, MAX_INPUT, 0);
		enableUpgrades(3); // slots 3 and 4: Speed and Efficiency upgrades
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ElectrolyzerBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		// Pull water from neighbouring sinks/tanks.
		if (be.water.getSpace() > 0) {
			for (Direction dir : Direction.values()) {
				BlockEntity neighbour = level.getBlockEntity(pos.relative(dir));
				if (neighbour == null) continue;
				IFluidHandler source = neighbour.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
				if (source != null) FluidUtil.tryFluidTransfer(be.water, source, new FluidStack(Fluids.WATER, PULL_WATER_PER_TICK), true);
			}
		}

		ItemStack cells = be.items.get(CELL_SLOT);
		ItemStack lithium = be.items.get(LITHIUM_SLOT);
		boolean tritium = !lithium.isEmpty();
		Item product = tritium ? ModBlocks.TRITIUM_CELL.get() : ModBlocks.DEUTERIUM_CELL.get();
		be.maxProgress = tritium ? TICKS_TRITIUM : TICKS_DEUTERIUM;

		boolean hasInputs = cells.is(ModBlocks.EMPTY_FUEL_CELL.get())
				&& (tritium || be.water.getFluidAmount() >= WATER_PER_DEUTERIUM);
		boolean working = hasInputs && be.canOutput(product) && be.energy.getEnergyStored() >= be.energyCost(ENERGY_PER_TICK);

		if (working) {
			be.energy.removeInternal(be.energyCost(ENERGY_PER_TICK));
			be.progress += be.progressStep(); // progress is stored in hundredths of a tick
			if (be.progress >= be.maxProgress * 100) {
				cells.shrink(1);
				if (tritium) {
					lithium.shrink(1);
				} else {
					be.water.drain(WATER_PER_DEUTERIUM, IFluidHandler.FluidAction.EXECUTE);
				}
				ItemStack out = be.items.get(OUTPUT_SLOT);
				if (out.isEmpty()) be.items.set(OUTPUT_SLOT, new ItemStack(product));
				else out.grow(1);
				be.progress = 0;
			}
			be.setChanged();
		} else if (be.progress != 0 && !hasInputs) {
			be.progress = 0;
			be.setChanged();
		}

		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	private boolean canOutput(Item product) {
		ItemStack out = items.get(OUTPUT_SLOT);
		return out.isEmpty() || (out.is(product) && out.getCount() < out.getMaxStackSize());
	}

	public static boolean isLithium(ItemStack stack) {
		return stack.is(ModTags.LITHIUM_INGOTS);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return waterCap.cast();
		return super.getCapability(cap, side);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		waterCap.invalidate();
	}

	@Override
	public void reviveCaps() {
		super.reviveCaps();
		waterCap = LazyOptional.of(() -> water);
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("water", water.writeToNBT(new CompoundTag()));
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water.readFromNBT(tag.getCompound("water"));
		progress = tag.getInt("progress");
	}

	// Hoppers/pipes: cells in from the top, lithium from the sides, finished cells out of the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
		if (side == Direction.UP) return new int[]{CELL_SLOT};
		return new int[]{LITHIUM_SLOT, CELL_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == CELL_SLOT) return stack.is(ModBlocks.EMPTY_FUEL_CELL.get());
		if (slot == LITHIUM_SLOT) return isLithium(stack);
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.electrolyzer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new ElectrolyzerMenu(containerId, inventory, this, data);
	}
}
