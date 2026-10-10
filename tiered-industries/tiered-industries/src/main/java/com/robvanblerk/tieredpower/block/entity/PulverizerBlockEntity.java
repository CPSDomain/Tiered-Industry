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
import com.robvanblerk.tieredpower.menu.PulverizerMenu;
import com.robvanblerk.tieredpower.recipe.PulverizingRecipe;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.registry.ModRecipes;

/** Grinds ores into two dusts (ore doubling), plus cobblestone -> gravel -> sand. Recipes come from JSON. */
public class PulverizerBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 20_000;
	public static final int MAX_INPUT = 1_000;
	public static final int ENERGY_PER_TICK = 40;
	public static final int INPUT_SLOT = 0, OUTPUT_SLOT = 1;

	private int progress;
	private int maxProgress = PulverizingRecipe.DEFAULT_TIME;

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
			return PulverizerMenu.DATA_COUNT;
		}
	};

	public PulverizerBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.PULVERIZER.get(), pos, state, 4, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(2); // slots 2 and 3: Speed and Efficiency upgrades
	}

	@Override
	public boolean supportsTiers() {
		return true;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		ItemStack input = be.items.get(INPUT_SLOT);
		Optional<PulverizingRecipe> recipe = input.isEmpty() ? Optional.empty()
				: level.getRecipeManager().getRecipeFor(ModRecipes.PULVERIZING.get(), new SimpleContainer(input), level);

		ItemStack result = recipe.map(r -> r.getResult()).orElse(ItemStack.EMPTY);
		int batch = result.isEmpty() ? 0 : Math.min(Math.min(be.lanes(), input.getCount()), roomFor(be.items.get(OUTPUT_SLOT), result));
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;

		if (working) {
			be.maxProgress = recipe.get().getTime();
			be.energy.removeInternal(cost);
			be.progress += be.progressStep(); // progress is stored in hundredths of a tick
			if (be.progress >= be.maxProgress * 100) {
				MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result.copyWithCount(result.getCount() * batch));
				input.shrink(batch);
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

	@Override
	public int[] getSlotsForFace(Direction side) {
		return side == Direction.DOWN ? new int[]{OUTPUT_SLOT} : new int[]{INPUT_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == INPUT_SLOT;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == INPUT_SLOT;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.pulverizer");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new PulverizerMenu(containerId, inventory, this, data);
	}
}
