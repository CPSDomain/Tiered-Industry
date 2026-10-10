package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.SteamTurbineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SteamTurbineMenu extends MachineMenu {
	public static final int DATA_COUNT = 5; // energy low, energy high, speed, steam, FE generated last tick

	private final ContainerLevelAccess access;

	public SteamTurbineMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainerData(DATA_COUNT), ContainerLevelAccess.NULL);
	}

	public SteamTurbineMenu(int containerId, Inventory inventory, ContainerData data, ContainerLevelAccess access) {
		super(ModMenus.STEAM_TURBINE.get(), containerId, data, 0);
		this.access = access;
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return SteamTurbineBlockEntity.ENERGY_CAPACITY;
	}

	public int getSpeed() { return data.get(2); }
	public int getSteam() { return data.get(3); }
	public int getGenerated() { return data.get(4); }

	@Override
	public boolean stillValid(Player player) {
		return stillValid(access, player, ModBlocks.STEAM_TURBINE.get());
	}
}
