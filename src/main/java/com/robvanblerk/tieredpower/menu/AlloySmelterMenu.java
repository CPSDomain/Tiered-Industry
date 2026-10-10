package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.AlloySmelterBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class AlloySmelterMenu extends MachineMenu {
	public static final int DATA_COUNT = 5;
	public static final int INPUT_A_X = 32, INPUT_B_X = 52, INPUT_Y = 35, OUTPUT_X = 112, OUTPUT_Y = 35;

	private final Container container;

	public AlloySmelterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(DATA_COUNT));
	}

	public AlloySmelterMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.ALLOY_SMELTER.get(), containerId, data, 5);
		this.container = container;
		addSlot(new Slot(container, AlloySmelterBlockEntity.INPUT_A, INPUT_A_X, INPUT_Y));
		addSlot(new Slot(container, AlloySmelterBlockEntity.INPUT_B, INPUT_B_X, INPUT_Y));
		addOutputSlot(container, AlloySmelterBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addUpgradeSlots(container, AlloySmelterBlockEntity.OUTPUT_SLOT + 1, 4);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return AlloySmelterBlockEntity.CAPACITY;
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
