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
import com.robvanblerk.tieredpower.menu.SaltEvaporatorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModBlocks;

/** Boils water down to salt: 1,000 mB of water makes 1 salt in 5 seconds, using 40 FE/t. Slots: 0 salt, 1-2 upgrades. */
public class SaltEvaporatorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000, MAX_INPUT = 2_000, ENERGY_PER_TICK = 40, TICKS = 100, WATER_PER_SALT = 1_000, TANK = 8_000;
	public static final int OUTPUT_SLOT = 0;
	private int water, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid().isSame(Fluids.WATER); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (!r.getFluid().isSame(Fluids.WATER)) return 0;
			int n = Math.min(r.getAmount(), TANK - water);
			if (a.execute() && n > 0) { water += n; setChanged(); }
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
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> water;
				case 5 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return SaltEvaporatorMenu.DATA_COUNT; }
	};

	public SaltEvaporatorBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SALT_EVAPORATOR.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(1);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SaltEvaporatorBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (be.water < TANK) be.water += GasTanks.pull(level, pos, Fluids.WATER, Math.min(1_000, TANK - be.water));
		ItemStack salt = new ItemStack(ModBlocks.SALT.get());
		int cost = be.energyCost(ENERGY_PER_TICK);
		boolean working = be.water >= WATER_PER_SALT && be.energy.getEnergyStored() >= cost && MachineBlockEntity.canMerge(be.items.get(OUTPUT_SLOT), salt);
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				be.water -= WATER_PER_SALT;
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, salt);
			}
			be.setChanged();
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return fluidCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); fluidCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); fluidCap = LazyOptional.of(() -> fluids); }
	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("water", water); tag.putInt("progress", progress); }
	@Override public void load(CompoundTag tag) { super.load(tag); water = tag.getInt("water"); progress = tag.getInt("progress"); }

	@Override public int[] getSlotsForFace(Direction side) { return new int[]{OUTPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return false; }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.salt_evaporator"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new SaltEvaporatorMenu(id, inv, this, data);
	}
}
