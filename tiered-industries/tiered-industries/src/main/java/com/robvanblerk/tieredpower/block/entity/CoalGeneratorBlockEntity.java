package com.robvanblerk.tieredpower.block.entity;

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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;

import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.energy.EnergyUtil;
import com.robvanblerk.tieredpower.menu.CoalGeneratorMenu;
import com.robvanblerk.tieredpower.registry.ModBlockEntities;

/**
 * Burns any furnace fuel (including fuels added by other mods) to make FE,
 * then pushes it into whatever is next to it: cables, batteries, or machines from any mod.
 */
public class CoalGeneratorBlockEntity extends MachineBlockEntity {
	public static final int CAPACITY = 50_000;
	public static final int OUTPUT_PER_TICK = 40;  // FE made per tick while burning
	public static final int MAX_PUSH = 1_000;      // FE pushed to each neighbour per tick

	/** FE per tick while burning, from the config (default 40). */
	public static int outputPerTick() {
		return com.robvanblerk.tieredpower.Config.get(com.robvanblerk.tieredpower.Config.COAL_GENERATOR_OUTPUT);
	}

	private int burnTime;
	private int burnTimeTotal;
	private int lastOutput; // FE sent to neighbours last tick, shown in the GUI

	private final ContainerData data = new ContainerData() {
		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> energy.getEnergyStored() & 0xFFFF;
				case 1 -> (energy.getEnergyStored() >>> 16) & 0xFFFF;
				case 2 -> burnTime;
				case 3 -> burnTimeTotal;
				case 4 -> lastOutput;
				default -> 0;
			};
		}

		@Override
		public void set(int index, int value) {}

		@Override
		public int getCount() {
			return CoalGeneratorMenu.DATA_COUNT;
		}
	};

	public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
		// 1 fuel slot. maxReceive = 0 so nothing can push power INTO a generator.
		super(ModBlockEntities.COAL_GENERATOR.get(), pos, state, 1, CAPACITY, 0, MAX_PUSH);
	}

	/** Uses the normal furnace burn time, so every vanilla and modded fuel works. */
	public static int burnTimeOf(ItemStack stack) {
		return stack.isEmpty() ? 0 : ForgeHooks.getBurnTime(stack, RecipeType.SMELTING);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity be) {
		if (be.preTick(level, pos, state)) return; // paused by redstone control
		boolean wasBurning = be.burnTime > 0;

		// Light a new fuel item when the last one has burned out and there's room for more energy.
		if (be.burnTime <= 0 && be.energy.getSpace() > 0) {
			ItemStack fuel = be.items.get(0);
			int time = burnTimeOf(fuel);
			if (time > 0) {
				be.burnTime = be.burnTimeTotal = time;
				if (fuel.hasCraftingRemainingItem() && fuel.getCount() == 1) {
					be.items.set(0, fuel.getCraftingRemainingItem()); // e.g. lava bucket -> empty bucket
				} else {
					fuel.shrink(1);
				}
				be.setChanged();
			}
		}

		// While the buffer is full the fire pauses, so no fuel is wasted.
		boolean room = be.energy.getSpace() >= com.robvanblerk.tieredpower.Config.gen(outputPerTick());
		if (be.burnTime > 0 && room) {
			be.burnTime--;
			be.energy.addInternal(com.robvanblerk.tieredpower.Config.gen(outputPerTick()));
		}

		// Push energy out of every side, and remember how much went out for the GUI.
		int pushed = 0;
		for (Direction dir : Direction.values()) {
			if (be.energy.getEnergyStored() <= 0) break;
			pushed += EnergyUtil.move(be.energy, EnergyUtil.neighbour(level, pos, dir), MAX_PUSH);
		}
		be.lastOutput = pushed;

		boolean isBurning = be.burnTime > 0 && room;
		if (state.getValue(MachineBlock.LIT) != isBurning) {
			level.setBlock(pos, state.setValue(MachineBlock.LIT, isBurning), Block.UPDATE_ALL);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag) {
		super.saveAdditional(tag);
		tag.putInt("burn_time", burnTime);
		tag.putInt("burn_time_total", burnTimeTotal);
	}

	@Override
	public void load(CompoundTag tag) {
		super.load(tag);
		burnTime = tag.getInt("burn_time");
		burnTimeTotal = tag.getInt("burn_time_total");
	}

	// Hoppers/pipes: fuel in from any side, nothing out (except empty buckets from the bottom).
	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{0};
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return burnTimeOf(stack) > 0;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return side == Direction.DOWN && burnTimeOf(stack) <= 0;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return burnTimeOf(stack) > 0;
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("block.tieredpower.coal_generator");
	}

	@Override
	public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new CoalGeneratorMenu(containerId, inventory, this, data);
	}
}
