package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.ElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ElectrolyzerMenu extends MachineMenu {
	public static final int DATA_COUNT = 6; // energy low, energy high, progress, max progress, water
	public static final int CELL_X = 30, CELL_Y = 26;
	public static final int LITHIUM_X = 30, LITHIUM_Y = 48;
	public static final int OUTPUT_X = 96, OUTPUT_Y = 37;

	private final Container container;

	public ElectrolyzerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(DATA_COUNT));
	}

	public ElectrolyzerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.ELECTROLYZER.get(), containerId, data, 5);
		this.container = container;
		addSlot(new Slot(container, ElectrolyzerBlockEntity.CELL_SLOT, CELL_X, CELL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return stack.is(ModBlocks.EMPTY_FUEL_CELL.get());
			}
		});
		addSlot(new Slot(container, ElectrolyzerBlockEntity.LITHIUM_SLOT, LITHIUM_X, LITHIUM_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ElectrolyzerBlockEntity.isLithium(stack);
			}
		});
		addOutputSlot(container, ElectrolyzerBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addUpgradeSlots(container, ElectrolyzerBlockEntity.OUTPUT_SLOT + 1, 5);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return ElectrolyzerBlockEntity.ENERGY_CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getWater() {
		return data.get(4);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
