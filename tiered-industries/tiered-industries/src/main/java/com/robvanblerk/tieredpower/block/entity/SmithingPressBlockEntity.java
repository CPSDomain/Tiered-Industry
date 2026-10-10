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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.menu.SmithingPressMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * A powered smithing table: runs vanilla (and other mods') smithing recipes - netherite upgrades, armour trims - so
 * autocrafting can do them through a processing pattern. Each item only goes into the slot a recipe uses it in.
 * Slots: 0 template, 1 base, 2 addition, 3 result, 4-5 upgrades.
 */
public class SmithingPressBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 40_000, MAX_INPUT = 2_000, ENERGY_PER_TICK = 60, TICKS = 100;
	public static final int TEMPLATE = 0, BASE = 1, ADDITION = 2, OUTPUT = 3;
	private int progress;

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int i) {
			return switch (i) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> progress / 100;
				case 3 -> TICKS;
				case 4 -> energyCost(ENERGY_PER_TICK);
				default -> 0;
			};
		}
		@Override public void set(int i, int v) {}
		@Override public int getCount() { return SmithingPressMenu.DATA_COUNT; }
	};

	public SmithingPressBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.SMITHING_PRESS.get(), pos, state, 6, CAPACITY, MAX_INPUT, 0);
		enableUpgrades(4);
	}

	/** Can this item go in this input slot - is it a template / base / addition in some smithing recipe? */
	public static boolean fitsSlot(@Nullable Level level, int slot, ItemStack stack) {
		if (level == null || stack.isEmpty() || slot > ADDITION) return false;
		for (SmithingRecipe r : level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)) {
			boolean ok = switch (slot) {
				case TEMPLATE -> r.isTemplateIngredient(stack);
				case BASE -> r.isBaseIngredient(stack);
				default -> r.isAdditionIngredient(stack);
			};
			if (ok) return true;
		}
		return false;
	}

	private Optional<SmithingRecipe> recipe(Level level, SimpleContainer c) {
		return level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, c, level);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, SmithingPressBlockEntity be) {
		if (be.preTick(level, pos, state)) return;
		SimpleContainer c = new SimpleContainer(be.items.get(TEMPLATE).copyWithCount(1), be.items.get(BASE).copyWithCount(1), be.items.get(ADDITION).copyWithCount(1));
		Optional<SmithingRecipe> r = c.isEmpty() ? Optional.empty() : be.recipe(level, c);
		ItemStack result = r.map(x -> x.assemble(c, level.registryAccess())).orElse(ItemStack.EMPTY);
		int batch = 0;
		if (!result.isEmpty()) {
			batch = be.lanes();
			for (int slot : new int[] {TEMPLATE, BASE, ADDITION}) if (!be.items.get(slot).isEmpty()) batch = Math.min(batch, be.items.get(slot).getCount());
			batch = Math.min(batch, roomFor(be.items.get(OUTPUT), result));
		}
		int cost = be.energyCost(ENERGY_PER_TICK) * Math.max(1, batch);
		boolean working = batch > 0 && be.energy.getEnergyStored() >= cost;
		if (working) {
			be.energy.removeInternal(cost);
			be.progress += be.progressStep();
			if (be.progress >= TICKS * 100) {
				be.progress = 0;
				MachineBlockEntity.merge(be.items, OUTPUT, result.copyWithCount(result.getCount() * batch));
				be.items.get(TEMPLATE).shrink(batch);
				be.items.get(BASE).shrink(batch);
				be.items.get(ADDITION).shrink(batch);
			}
			be.setChanged();
		} else if (result.isEmpty()) {
			be.progress = 0;
		}
		if (state.getValue(MachineBlock.LIT) != working) level.setBlock(pos, state.setValue(MachineBlock.LIT, working), Block.UPDATE_ALL);
	}

	@Override public boolean supportsTiers() { return true; }

	@Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); tag.putInt("progress", progress); }
	@Override public void load(CompoundTag tag) { super.load(tag); progress = tag.getInt("progress"); }

	@Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? new int[]{OUTPUT} : new int[]{TEMPLATE, BASE, ADDITION}; }
	@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
	@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT; }
	@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot <= ADDITION && fitsSlot(level, slot, stack); }
	@Override public Component getDisplayName() { return Component.translatable("block.tieredpower.smithing_press"); }

	@Override
	public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
		return new SmithingPressMenu(id, inv, this, data);
	}
}
