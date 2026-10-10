package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

import com.robvanblerk.tieredpower.block.entity.ProcessingMachineBlockEntity;

/** Menu shared by the Ore Purifier, Compressor and Electric Sawmill. */
public class ProcessingMachineMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max progress, 4 FE/t, 5 water
	public static final int DATA_COUNT = 6;
	public static final int INPUT_X = 30, SLOT_Y = 35, OUTPUT_X = 90, SECONDARY_X = 116;

	private final Container container;
	private final boolean usesWater;

	/** Client side. */
	public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory inventory, boolean usesWater) {
		this(type, containerId, inventory, new SimpleContainer(5), new SimpleContainerData(DATA_COUNT), usesWater);
	}

	public ProcessingMachineMenu(MenuType<?> type, int containerId, Inventory inventory, Container container, ContainerData data, boolean usesWater) {
		super(type, containerId, data, 5);
		this.container = container;
		this.usesWater = usesWater;
		addSlot(new Slot(container, ProcessingMachineBlockEntity.INPUT_SLOT, INPUT_X, SLOT_Y));
		addOutputSlot(container, ProcessingMachineBlockEntity.OUTPUT_SLOT, OUTPUT_X, SLOT_Y, true);
		addOutputSlot(container, ProcessingMachineBlockEntity.SECONDARY_SLOT, SECONDARY_X, SLOT_Y, false);
		addUpgradeSlots(container, 3, 4);
		addPlayerInventory(inventory);
	}

	public boolean usesWater() {
		return usesWater;
	}

	public int getWater() {
		return data.get(5);
	}

	@Override
	public long getCapacity() {
		return ProcessingMachineBlockEntity.CAPACITY;
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
