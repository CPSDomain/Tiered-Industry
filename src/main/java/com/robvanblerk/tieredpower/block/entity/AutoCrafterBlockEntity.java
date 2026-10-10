package com.robvanblerk.tieredpower.block.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.AutoCrafterMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;
import com.robvanblerk.tieredpower.util.ItemUtil;

/**
 * Crafts a recipe over and over. Set the recipe by clicking items into the 3x3 pattern grid (ghost items - they aren't
 * used up). Ingredients are pulled from chests/inventories next to it; the result goes into its output slot, which
 * hoppers and pipes can empty. Slots: 0 output, 1-2 upgrades. The pattern is stored separately.
 */
public class AutoCrafterBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000;
	public static final int MAX_INPUT = 2_000;
	public static final int ENERGY_PER_TICK = 40;
	public static final int TICKS_PER_CRAFT = 20;
	public static final int OUTPUT_SLOT = 0;

	/** Needed by vanilla's crafting grid; never shown. */
	private static final AbstractContainerMenu DUMMY_MENU = new AbstractContainerMenu(null, -1) {
		@Override
		public ItemStack quickMoveStack(Player player, int index) {
			return ItemStack.EMPTY;
		}

		@Override
		public boolean stillValid(Player player) {
			return false;
		}
	};

	private final SimpleContainer pattern = new SimpleContainer(9);
	private int progress;
	private int status; // 0 = no recipe, 1 = missing ingredients, 2 = output full, 3 = crafting

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> TICKS_PER_CRAFT;
				case 4 -> status;
				case 5 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}

		@Override
		public void set(int i, int value) {}

		@Override
		public int getCount() {
			return AutoCrafterMenu.DATA_COUNT;
		}
	};

	public AutoCrafterBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.AUTO_CRAFTER.get(), pos, state, 3, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(1);
		pattern.addListener(c -> setChanged());
	}

	public SimpleContainer getPattern() {
		return pattern;
	}

	private TransientCraftingContainer grid() {
		TransientCraftingContainer grid = new TransientCraftingContainer(DUMMY_MENU, 3, 3);
		for (int i = 0; i < 9; i++) grid.setItem(i, pattern.getItem(i).copyWithCount(1));
		return grid;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, AutoCrafterBlockEntity be) {
		if (be.preTick(level, pos, state)) return;

		TransientCraftingContainer grid = be.grid();
		Optional<CraftingRecipe> recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
		ItemStack result = recipe.map(r -> r.assemble(grid, level.registryAccess())).orElse(ItemStack.EMPTY);

		boolean working = false;
		if (result.isEmpty()) {
			be.status = 0;
			be.progress = 0;
		} else if (!MachineBlockEntity.canMerge(be.items.get(OUTPUT_SLOT), result)) {
			be.status = 2;
		} else if (!be.hasIngredients(level, pos, false)) {
			be.status = 1;
		} else {
			int cost = be.energyCost(ENERGY_PER_TICK);
			if (be.energy.getEnergyStored() >= cost) {
				working = true;
				be.status = 3;
				be.energy.removeInternal(cost);
				be.progress += be.progressStep();
				if (be.progress >= TICKS_PER_CRAFT * 100) {
					be.progress = 0;
					if (be.hasIngredients(level, pos, true)) {
						MachineBlockEntity.merge(be.items, OUTPUT_SLOT, result);
						// Containers left behind (e.g. empty buckets) go back into a neighbouring inventory.
						for (ItemStack leftover : recipe.get().getRemainingItems(grid)) {
							if (leftover.isEmpty()) continue;
							ItemStack rest = ItemUtil.pushToNeighbours(level, pos, leftover.copy());
							if (!rest.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, rest);
						}
					}
				}
				be.setChanged();
			}
		}
		if (state.getValue(MachineBlock.LIT) != working) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
		}
	}

	/** Checks (or, if take is true, removes) one of each pattern item from neighbouring inventories. */
	private boolean hasIngredients(Level level, BlockPos pos, boolean take) {
		List<ItemStack> needed = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			ItemStack want = pattern.getItem(i);
			if (want.isEmpty()) continue;
			boolean merged = false;
			for (ItemStack n : needed) {
				if (ItemStack.isSameItemSameTags(n, want)) {
					n.grow(1);
					merged = true;
					break;
				}
			}
			if (!merged) needed.add(want.copyWithCount(1));
		}

		for (ItemStack want : needed) {
			int missing = want.getCount();
			for (Direction dir : Direction.values()) {
				IItemHandler inv = ItemUtil.neighbour(level, pos, dir);
				if (inv == null) continue;
				for (int s = 0; s < inv.getSlots() && missing > 0; s++) {
					ItemStack have = inv.getStackInSlot(s);
					if (!ItemStack.isSameItemSameTags(have, want)) continue;
					ItemStack got = inv.extractItem(s, missing, !take);
					missing -= got.getCount();
				}
				if (missing <= 0) break;
			}
			if (missing > 0) return false; // (when taking, the pre-check just passed, so this won't leave things half-taken)
		}
		return true;
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.put("pattern", pattern.createTag());
		tag.putInt("progress", progress);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		pattern.fromTag(tag.getList("pattern", 10));
		progress = tag.getInt("progress");
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{OUTPUT_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot == OUTPUT_SLOT;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.auto_crafter");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new AutoCrafterMenu(containerId, inventory, this, data, pattern);
	}
}
