package com.robvanblerk.tieredpower.block.entity;

import java.util.Optional;

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

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.AlloySmelterMenu;
import com.robvanblerk.tieredpower.recipe.AlloyingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/** Combines two inputs into an alloy (steel, superconductor, ...). Inputs can go in either slot. */
public class AlloySmelterBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000;
	public static final int MAX_INPUT = 2_000;
	public static final int ENERGY_PER_TICK = 60;
	public static final int INPUT_A = 0, INPUT_B = 1, OUTPUT_SLOT = 2;

	private int progress;
	private int maxProgress = AlloyingRecipe.DEFAULT_TIME;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> maxProgress;
				case 4 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return AlloySmelterMenu.DATA_COUNT;
		}
	};

	public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state, 5, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(3); // slots 3 and 4: Speed and Efficiency upgrades
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		ItemStack a = be.items.get(INPUT_A), b = be.items.get(INPUT_B);
		Optional<AlloyingRecipe> recipe = (a.isEmpty() || b.isEmpty()) ? Optional.empty()
				: level.getRecipeManager().getRecipeFor(ModRecipes.ALLOYING.get(), new SimpleContainer(a, b), level);

		ItemStack result = recipe.map(r -> r.getResult()).orElse(ItemStack.EMPTY);
		// How many run side by side: the tier's lanes, limited by both ingredients and output room.
		int[] per = result.isEmpty() ? null : recipe.get().consumption(a, b);
		int batch = 0;
		if (per != null) {
			batch = be.lanes();
			if (per[0] > 0) batch = Math.min(batch, a.getCount() / per[0]);
			if (per[1] > 0) batch = Math.min(batch, b.getCount() / per[1]);
			batch = Math.min(batch, roomFor(be.items.get(OUTPUT_SLOT), result));
		}
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;

		if (working) {
			be.maxProgress = recipe.get().getTime();
			be.energy.removeInternal(cost);
			be.progress += be.progressStep(); // progress is stored in hundredths of a tick
			if (be.progress >= be.maxProgress * 100) {
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				a.shrink(per[0] * batch);
				b.shrink(per[1] * batch);
				be.progress = 0;
			}
			be.setChanged();
		} else if (be.progress != 0 && result.isEmpty()) {
			be.progress = 0;
			be.setChanged();
		}

		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		progress = tag.getInt("progress");
	}

	// Hoppers/pipes: first input from the top, second input from the sides, output from the bottom.
	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.DOWN) return new int[]{OUTPUT_SLOT};
		if (side == Direction.UP) return new int[]{INPUT_A};
		return new int[]{INPUT_B};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot != OUTPUT_SLOT;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot != OUTPUT_SLOT;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.alloy_smelter");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new AlloySmelterMenu(containerId, inventory, this, data);
	}

	/**
	 * Automation fills the two input slots with the two different ingredients of a recipe: an item stacks onto its own
	 * slot, never takes the second slot while the first already holds it, and only goes in if it makes a recipe with
	 * whatever is already loaded. So lithium from one pipe and copper from another each land in their own slot.
	 */
	@Override
	protected boolean acceptsAutomatedInput(int slot, ItemStack stack) {
		if (slot != INPUT_A && slot != INPUT_B) return true;
		ItemStack here = items.get(slot);
		ItemStack other = items.get(slot == INPUT_A ? INPUT_B : INPUT_A);
		if (!here.isEmpty()) return ItemStack.isSameItemSameTags(here, stack);
		if (!other.isEmpty() && ItemStack.isSameItemSameTags(other, stack)) return false; // keep this slot for the partner
		if (level == null) return false;
		for (com.robvanblerk.tieredpower.recipe.AlloyingRecipe r : level.getRecipeManager().getAllRecipesFor(com.robvanblerk.tieredpower.registry.ModRecipes.ALLOYING.get())) {
			if (other.isEmpty()) {
				if (r.getFirst().test(stack) || r.getSecond().test(stack)) return true;
			} else if ((r.getFirst().test(stack) && r.getSecond().test(other)) || (r.getSecond().test(stack) && r.getFirst().test(other))) {
				return true;
			}
		}
		return false;
	}
}
