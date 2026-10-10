package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class WindTurbineMenu extends MachineMenu {
	// 0-1 energy, 2 FE/t, 3 Y, 4 clearance %, 5 weather
	public static final int DATA_COUNT = 6;

	public WindTurbineMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainerData(DATA_COUNT));
	}

	public WindTurbineMenu(int containerId, Inventory inventory, ContainerData data) {
		super(ModMenus.WIND_TURBINE.get(), containerId, data, 0);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return WindTurbineBlockEntity.CAPACITY;
	}

	public int getGenerating() { return data.get(2); }
	public int getY() { return (short) data.get(3); }
	public int getClearance() { return data.get(4); }
	public int getWeather() { return data.get(5); }

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
