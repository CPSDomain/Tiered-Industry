package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.WorkerBlockEntity;

/** Shared menu for the automation machines: 3 inputs, 3x3 outputs, upgrades. */
public class WorkerMenu extends MachineMenu {
	// 0-1 energy, 2 actions, 3 FE per job, 4 status, 5 machine-specific message
	public static final int DATA_COUNT = 6;
	public static final int INPUT_X = 30, GRID_X = 98, TOP_Y = 17;
	private final Container container;

	public WorkerMenu(MenuType<WorkerMenu> type, int id, Inventory inv) {
		this(type, id, inv, new SimpleContainer(WorkerBlockEntity.SIZE), new SimpleContainerData(DATA_COUNT));
	}

	public WorkerMenu(MenuType<WorkerMenu> type, int id, Inventory inv, Container container, ContainerData data) {
		super(type, id, data, WorkerBlockEntity.SIZE);
		this.container = container;
		for (int i = 0; i < WorkerBlockEntity.INPUTS; i++) {
			addSlot(new Slot(container, i, INPUT_X, TOP_Y + i * 18) {
				@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(getContainerSlot(), s); }
			});
		}
		for (int i = 0; i < 9; i++) addOutputSlot(container, WorkerBlockEntity.OUTPUT_START + i, GRID_X + (i % 3) * 18, TOP_Y + (i / 3) * 18, false);
		addUpgradeSlots(container, WorkerBlockEntity.UPGRADE_SLOT, 3);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return getType() == com.robvanblerk.tieredpower.registry.ModMenus.LASER_DRILL.get() ? com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity.CAPACITY : WorkerBlockEntity.CAPACITY; }
	public int getActions() { return data.get(2) & 0xFFFF; }
	public int getStatus() { return data.get(4); }
	public int getSpecial() { return data.get(5); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
