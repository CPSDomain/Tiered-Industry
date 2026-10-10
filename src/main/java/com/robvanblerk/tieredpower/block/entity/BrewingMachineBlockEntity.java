package com.robvanblerk.tieredpower.block.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.BrewingMachineMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * An electric brewing stand for automation: 3 bottle slots, an ingredient slot and 3 output slots. It fills glass bottles
 * with water from its own tank (333 mB each), then brews all three bottles with one ingredient (5 s, 20 FE/t) using the
 * game's brewing recipes - every vanilla potion and modded brewing too. No blaze powder needed.
 * Slots: 0-2 bottles, 3 ingredient, 4-6 outputs, 7-8 upgrades.
 */
public class BrewingMachineBlockEntity extends MachineBlockEntity {
	public static final int BOTTLES = 3, INGREDIENT = 3, OUT = 4, SIZE = 9, UPGRADE_SLOT = 7;
	public static final int CAPACITY = 50_000, MAX_INPUT = 1_000, ENERGY_PER_TICK = 20, TIME = 100, TANK = 4_000, WATER_PER_BOTTLE = 333;
	private int progress;
	public final FluidTank water = new FluidTank(TANK, f -> f.getFluid().isSame(Fluids.WATER)) {
		@Override protected void onContentsChanged() { setChanged(); }
	};
	private LazyOptional<IFluidHandler> waterCap = LazyOptional.of(() -> water);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress;
				case 3 -> TIME * 100;
				case 4 -> energyCost(ENERGY_PER_TICK);
				case 5 -> water.getFluidAmount();
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return BrewingMachineMenu.DATA_COUNT; }
	};

	public BrewingMachineBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.BREWING_MACHINE.get(), pos, state, SIZE, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(UPGRADE_SLOT);
	}

	private NonNullList<ItemStack> bottles() {
		NonNullList<ItemStack> list = NonNullList.withSize(BOTTLES, ItemStack.EMPTY);
		for (int i = 0; i < BOTTLES; i++) list.set(i, items.get(i));
		return list;
	}

	private boolean outputsEmpty() {
		for (int i = OUT; i < OUT + 3; i++) if (!items.get(i).isEmpty()) return false;
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BrewingMachineBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		// Fill glass bottles with water from the tank.
		for (int i = 0; i < BOTTLES; i++) {
			if (be.items.get(i).is(Items.GLASS_BOTTLE) && be.water.getFluidAmount() >= WATER_PER_BOTTLE) {
				be.water.drain(WATER_PER_BOTTLE, IFluidHandler.FluidAction.EXECUTE);
				be.items.set(i, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER));
			}
		}
		ItemStack ingredient = be.items.get(INGREDIENT);
		NonNullList<ItemStack> bottles = be.bottles();
		boolean canBrew = !ingredient.isEmpty() && be.outputsEmpty() && BrewingRecipeRegistry.canBrew(bottles, ingredient, new int[]{0, 1, 2});
		boolean working = false;
		int cost = be.energyCost(ENERGY_PER_TICK);
		if (canBrew && be.energy.getEnergyStored() >= cost) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			working = true;
			if (be.progress >= TIME * 100) {
				be.progress = 0;
				BrewingRecipeRegistry.brewPotions(bottles, ingredient, new int[]{0, 1, 2});
				for (int i = 0; i < BOTTLES; i++) {
					be.items.set(OUT + i, bottles.get(i));
					be.items.set(i, ItemStack.EMPTY);
				}
				ItemStack remainder = ingredient.getCraftingRemainingItem();
				ingredient.shrink(1);
				if (ingredient.isEmpty() && !remainder.isEmpty()) be.items.set(INGREDIENT, remainder);
			}
		} else if (!canBrew) {
			be.progress = 0;
		}
		be.setChanged();
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot < BOTTLES) return items.get(slot).isEmpty() && (stack.is(Items.GLASS_BOTTLE) || BrewingRecipeRegistry.isValidInput(stack));
		if (slot == INGREDIENT) return BrewingRecipeRegistry.isValidIngredient(stack);
		return false;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{4, 5, 6} : new int[]{0, 1, 2, 3};
	}

	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= OUT && slot < OUT + 3; }
	@Override public int getMaxStackSize() { return 64; }

	@Override
	public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
		if (cap == ForgeCapabilities.FLUID_HANDLER) return waterCap.cast();
		return super.getCapability(cap, side);
	}

	@Override public void invalidateCaps() { super.invalidateCaps(); waterCap.invalidate(); }
	@Override public void reviveCaps() { super.reviveCaps(); waterCap = LazyOptional.of(() -> water); }

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("progress", progress);
		tag.put("water", water.writeToNBT(new CompoundTag()));
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		progress = tag.getInt("progress");
		water.readFromNBT(tag.getCompound("water"));
	}

	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.brewing_machine"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new BrewingMachineMenu(id, inv, this, data);
	}
}
