package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;

import com.robvanblerk.tieredpower.block.entity.CryogenicCondenserBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FuelRefineryBlockEntity;

/** Shared by the Cryogenic Condenser and the Fuel Refinery: three tanks, power, upgrades. */
public class RocketFuelMenu extends MachineMenu {
	// 0-1 energy, 2 first input, 3 second input, 4 output, 5 condenser gas kind (1 methane, 2 oxygen), 6 condenser liquid kind
	public static final int DATA_COUNT = 7;
	private final Container container;
	private final boolean refinery;

	public RocketFuelMenu(MenuType<RocketFuelMenu> type, int id, Inventory inv, boolean refinery) {
		this(type, id, inv, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT), refinery);
	}

	public RocketFuelMenu(MenuType<RocketFuelMenu> type, int id, Inventory inv, Container container, ContainerData data, boolean refinery) {
		super(type, id, data, 2);
		this.container = container;
		this.refinery = refinery;
		addUpgradeSlots(container, 0, -1);
		addPlayerInventory(inv);
	}

	public boolean isRefinery() { return refinery; }
	public int tankSize() { return refinery ? FuelRefineryBlockEntity.TANK : CryogenicCondenserBlockEntity.TANK; }
	public int get(int i) { return data.get(i); }
	@Override public long getCapacity() { return refinery ? FuelRefineryBlockEntity.CAPACITY : CryogenicCondenserBlockEntity.CAPACITY; }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
