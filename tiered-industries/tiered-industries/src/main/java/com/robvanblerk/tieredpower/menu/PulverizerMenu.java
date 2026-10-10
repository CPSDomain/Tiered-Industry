package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.PulverizerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class PulverizerMenu extends MachineMenu {
	public static final int DATA_COUNT = 5; // energy low, energy high, progress, max progress
	public static final int INPUT_X = 44, INPUT_Y = 35, OUTPUT_X = 104, OUTPUT_Y = 35;

	private final Container container;

	public PulverizerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public PulverizerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.PULVERIZER.get(), containerId, data, 4);
		this.container = container;
		addSlot(new Slot(container, PulverizerBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y));
		addOutputSlot(container, PulverizerBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addUpgradeSlots(container, PulverizerBlockEntity.OUTPUT_SLOT + 1, 4);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return PulverizerBlockEntity.CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
