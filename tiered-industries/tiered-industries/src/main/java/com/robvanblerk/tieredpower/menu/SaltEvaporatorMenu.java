package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.SaltEvaporatorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SaltEvaporatorMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 water, 5 FE/t
	public static final int DATA_COUNT = 6;
	private final Container container;

	public SaltEvaporatorMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public SaltEvaporatorMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.SALT_EVAPORATOR.get(), id, data, 3);
		this.container = container;
		addOutputSlot(container, 0, 104, 35, true);
		addUpgradeSlots(container, 1, 5);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return SaltEvaporatorBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getWater() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
