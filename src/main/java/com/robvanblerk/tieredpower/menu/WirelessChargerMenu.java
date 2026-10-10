package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.WirelessChargerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class WirelessChargerMenu extends MachineMenu {
	// 0-1 energy, 2 players in range, 3-4 FE/t being sent
	public static final int DATA_COUNT = 5;

	public WirelessChargerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainerData(DATA_COUNT));
	}

	public WirelessChargerMenu(int containerId, Inventory inventory, ContainerData data) {
		super(ModMenus.WIRELESS_CHARGER.get(), containerId, data, 0);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return WirelessChargerBlockEntity.CAPACITY;
	}

	public int getPlayers() { return data.get(2); }
	public int getRate() { return ((data.get(4) & 0xFFFF) << 16) | (data.get(3) & 0xFFFF); }

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}
