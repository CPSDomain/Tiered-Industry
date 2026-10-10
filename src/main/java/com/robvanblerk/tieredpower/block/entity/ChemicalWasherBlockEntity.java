package com.robvanblerk.tieredpower.block.entity;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.ChemicalWasherMenu;
import com.robvanblerk.tieredpower.recipe.MachineRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/**
 * The top tier of ore processing: chlorine dissolves the metal out of an ore, giving one more dust than the Ore
 * Purifier (4 per ore). 1 ore or raw ore + 100 mB chlorine + 500 mB water per operation, 6 seconds at 300 FE/t. Works on
 * anything the Ore Purifier handles that is tagged as an ore or raw ore - other mods' ores included. Tierable.
 * Slots: 0 ore, 1 dust, 2-3 upgrades.
 */
public class ChemicalWasherBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 120_000, MAX_INPUT = 8_000, ENERGY_PER_TICK = 300, TICKS = 120, TANK = 8_000;
	public static final int CHLORINE_PER_OP = 100, WATER_PER_OP = 500, INPUT_SLOT = 0, OUTPUT_SLOT = 1;
	private int water, chlorine, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 2; }
		@Override public @NotNull FluidStack getFluidInTank(int t) {
			if (t == 0) return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
			return chlorine > 0 ? new FluidStack(ModFluids.CHLORINE.get(), chlorine) : FluidStack.EMPTY;
		}
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return t == 0 ? s.getFluid().isSame(Fluids.WATER) : s.getFluid() == ModFluids.CHLORINE.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			boolean w = r.getFluid().isSame(Fluids.WATER);
			if (!w && r.getFluid() != ModFluids.CHLORINE.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - (w ? water : chlorine));
			if (a.execute() && n > 0) {
				if (w) water += n; else chlorine += n;
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
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> water;
				case 5 -> chlorine;
				case 6 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return ChemicalWasherMenu.DATA_COUNT; }
	};

	public ChemicalWasherBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CHEMICAL_WASHER.get(), pos, state, 4, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(2);
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	/** What one ore washes into: the Ore Purifier's result plus one more. Empty if it isn't an ore. */
	public static ItemStack washResult(Level level, ItemStack ore) {
		// Nuclear reprocessing: chlorine dissolves the plutonium out of a spent rod.
		if (ore.is(com.robvanblerk.tieredpower.registry.ModBlocks.DEPLETED_FUEL_ROD.get())) return new ItemStack(com.robvanblerk.tieredpower.registry.ModBlocks.PLUTONIUM_DUST.get());
		if (ore.isEmpty() || !(ore.is(Tags.Items.ORES) || ore.is(Tags.Items.RAW_MATERIALS))) return ItemStack.EMPTY;
		Optional<MachineRecipe> r = level.getRecipeManager().getRecipeFor(ModRecipes.PURIFYING.get(), new SimpleContainer(ore.copyWithCount(64)), level);
		if (r.isEmpty() || r.get().getCount() != 1) return ItemStack.EMPTY;
		ItemStack base = r.get().getResult();
		return base.isEmpty() ? ItemStack.EMPTY : base.copyWithCount(base.getCount() + 1);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ChemicalWasherBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		if (be.water < TANK) be.water += GasTanks.pull(level, pos, Fluids.WATER, Math.min(1_000, TANK - be.water));
		ItemStack ore = be.items.get(INPUT_SLOT);
		ItemStack result = washResult(level, ore);
		int batch = 0;
		if (!result.isEmpty()) {
			batch = Math.min(be.lanes(), ore.getCount());
			batch = Math.min(batch, roomFor(be.items.get(OUTPUT_SLOT), result));
			batch = Math.min(batch, be.water / WATER_PER_OP);
			batch = Math.min(batch, be.chlorine / CHLORINE_PER_OP);
		}
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				ore.shrink(batch);
				be.water -= WATER_PER_OP * batch;
				be.chlorine -= CHLORINE_PER_OP * batch;
			}
			be.setChanged();
		} else if (result.isEmpty()) {
			be.progress = 0;
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

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("water", water);
		tag.putInt("chlorine", chlorine);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		water = tag.getInt("water");
		chlorine = tag.getInt("chlorine");
		progress = tag.getInt("progress");
	}

	@Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT && level != null && !washResult(level, stack).isEmpty(); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.chemical_washer"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new ChemicalWasherMenu(id, inv, this, data);
	}
}
