package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.BoilerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.CoalGeneratorBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class BoilerMenu extends MachineMenu {
	// 0-1 unused energy, 2 water, 3 steam, 4 burn time, 5 burn total, 6 heat %, 7 steam sent/t, 8 lava
	public static final int DATA_COUNT = 9;
	public static final int FUEL_X = 26, FUEL_Y = 53;

	private final Container container;

	public BoilerMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public BoilerMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.BOILER.get(), containerId, data, 1);
		this.container = container;
		addSlot(new Slot(container, 0, FUEL_X, FUEL_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return CoalGeneratorBlockEntity.burnTimeOf(stack) > 0;
			}
		});
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return 0;
	}

	public int getWater() { return data.get(2); }
	public int getSteam() { return data.get(3); }
	public int getHeat() { return data.get(6); }
	public int getSteamSent() { return data.get(7); }
	public int getLava() { return data.get(8); }

	public float getBurnFraction() {
		int total = data.get(5);
		return total <= 0 ? 0f : (float) data.get(4) / total;
	}

	public float getWaterFraction() {
		return (float) getWater() / BoilerBlockEntity.WATER_CAPACITY;
	}

	public float getLavaFraction() {
		return (float) getLava() / BoilerBlockEntity.LAVA_CAPACITY;
	}

	public float getSteamFraction() {
		return (float) getSteam() / BoilerBlockEntity.STEAM_CAPACITY;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
