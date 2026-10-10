package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.IsotopeSeparatorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class IsotopeSeparatorMenu extends MachineMenu {
	// 0-1 energy, 2 water, 3 deuterium, 4 FE/t
	public static final int DATA_COUNT = 5;
	private final Container container;

	public IsotopeSeparatorMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public IsotopeSeparatorMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.ISOTOPE_SEPARATOR.get(), id, data, 2);
		this.container = container;
		addUpgradeSlots(container, 0, 4);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return IsotopeSeparatorBlockEntity.CAPACITY; }
	public int getWater() { return data.get(2); }
	public int getDeuterium() { return data.get(3); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
