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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.SteamHammerMenu;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModFluids;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/**
 * Runs Pulverizer recipes on steam instead of power: 20 mB of steam per tick, 7.5 seconds per item (Speed Upgrades help).
 * A good use for leftover reactor or boiler steam. Slots: 0 input, 1 output, 2-3 upgrades.
 */
public class SteamHammerBlockEntity extends MachineBlockEntity {
	public static final int TANK = 8_000, STEAM_PER_TICK = 20, TICKS = 150;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;

	private int steam, progress;

	private final IFluidHandler fluids = new IFluidHandler() {
		@Override public int getTanks() { return 1; }
		@Override public @NotNull FluidStack getFluidInTank(int t) { return steam > 0 ? new FluidStack(ModFluids.STEAM.get(), steam) : FluidStack.EMPTY; }
		@Override public int getTankCapacity(int t) { return TANK; }
		@Override public boolean isFluidValid(int t, @NotNull FluidStack s) { return s.getFluid() == ModFluids.STEAM.get(); }

		@Override
		public int fill(FluidStack r, FluidAction a) {
			if (r.getFluid() != ModFluids.STEAM.get()) return 0;
			int n = Math.min(r.getAmount(), TANK - steam);
			if (a.execute() && n > 0) { steam += n; setChanged(); }
			return n;
		}

		@Override public @NotNull FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
		@Override public @NotNull FluidStack drain(int m, FluidAction a) { return FluidStack.EMPTY; }
	};
	private LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluids);

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0, 1 -> 0;
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> steam;
				default -> 0;
			};
		}

		@Override public void set(int i, int v) {}
		@Override public int getCount() { return SteamHammerMenu.DATA_COUNT; }
	};

	public SteamHammerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.STEAM_HAMMER.get(), pos, state, 4, 1, 0, 0);
		enableUpgrades(2);
	}

	@Override
	protected boolean exposesEnergy() {
		return false; // runs on steam
	}

	private Optional<PulverizingRecipe> recipe(Level level) {
		return level.getRecipeManager().getRecipeFor(ModRecipes.PULVERIZING.get(), new SimpleContainer(items.get(INPUT_SLOT)), level);
	}

	public static boolean canProcess(Level level, ItemStack stack) {
		return level.getRecipeManager().getRecipeFor(ModRecipes.PULVERIZING.get(), new SimpleContainer(stack), level).isPresent();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SteamHammerBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		Optional<PulverizingRecipe> recipe = be.items.get(INPUT_SLOT).isEmpty() ? Optional.empty() : be.recipe(level);
		ItemStack result = recipe.map(r -> r.getResultItem(level.registryAccess())).orElse(ItemStack.EMPTY);
		int use = STEAM_PER_TICK * be.progressStep() / 100;
		boolean working = !result.isEmpty() && be.steam >= use && MachineBlockEntity.canMerge(be.items.get(OUTPUT_SLOT), result);
		if (working) {
			be.steam -= use;
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copy());
				be.items.get(INPUT_SLOT).shrink(1);
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
		tag.putInt("steam", steam);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		steam = tag.getInt("steam");
		progress = tag.getInt("progress");
	}

	@Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT_SLOT; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == INPUT_SLOT && level != null && canProcess(level, stack); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.steam_hammer"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new SteamHammerMenu(id, inv, this, data);
	}
}
