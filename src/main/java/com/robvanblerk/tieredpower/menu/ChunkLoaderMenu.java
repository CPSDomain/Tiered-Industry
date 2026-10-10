package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.ChunkLoaderBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ChunkLoaderMenu extends MachineMenu {
	// 0-1 energy, 2 radius, 3 loading, 4 FE/t, 5 largest radius for its tier
	public static final int DATA_COUNT = 6;

	private final Container container;

	public ChunkLoaderMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public ChunkLoaderMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.CHUNK_LOADER.get(), containerId, data, 2);
		this.container = container;
		addUpgradeSlots(container, 0, 4);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return ChunkLoaderBlockEntity.CAPACITY;
	}

	public int getRadius() { return data.get(2); }
	public boolean isLoading() { return data.get(3) != 0; }
	public int getCost() { return data.get(4); }
	public int getMaxRadius() { return data.get(5); }

	/** Button 1 / 2 make the loaded area bigger / smaller (button 0 is redstone control). */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if ((id == 1 || id == 2) && container instanceof ChunkLoaderBlockEntity loader) {
			loader.cycleRadius(id == 2);
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
