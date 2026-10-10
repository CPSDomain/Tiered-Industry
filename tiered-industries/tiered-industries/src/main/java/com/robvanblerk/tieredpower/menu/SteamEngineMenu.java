package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SteamEngineMenu extends MachineMenu {
	// 0-1 energy, 2 steam, 3 FE/t
	public static final int DATA_COUNT = 4;

	public SteamEngineMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainerData(DATA_COUNT));
	}

	public SteamEngineMenu(int id, Inventory inv, ContainerData data) {
		super(ModMenus.STEAM_ENGINE.get(), id, data, 0);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return SteamEngineBlockEntity.CAPACITY; }
	public int getSteam() { return data.get(2); }
	public int getGenerating() { return data.get(3); }
	@Override public boolean stillValid(Player player) { return true; }
}
