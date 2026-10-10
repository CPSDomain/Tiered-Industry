package com.robvanblerk.tieredpower.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import com.robvanblerk.tieredpower.block.entity.ElectricPumpBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ElectricPumpMenu extends MachineMenu {
	// 0-1 energy, 2 fluid amount (low bits), 3 fluid id (-1 empty), 4 no fluid below, 5 FE/t, 6 fluid amount (high bits),
	// 7 tank capacity in buckets
	public static final int DATA_COUNT = 8;

	private final Container container;

	public ElectricPumpMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public ElectricPumpMenu(int containerId, Inventory inventory, Container container, ContainerData data) {
		super(ModMenus.ELECTRIC_PUMP.get(), containerId, data, 2);
		this.container = container;
		addUpgradeSlots(container, 0, 5);
		addPlayerInventory(inventory);
	}

	@Override
	public long getCapacity() {
		return ElectricPumpBlockEntity.CAPACITY;
	}

	public int getFluidAmount() { return (data.get(2) & 0xFFFF) | ((data.get(6) & 0xFFFF) << 16); }
	public int getTankCapacity() { return Math.max(1, data.get(7)) * 1000; }
	public boolean noFluidBelow() { return data.get(4) != 0; }

	public Fluid getFluid() {
		int id = data.get(3);
		if (id == 0xFFFF || id < 0) return Fluids.EMPTY;
		Fluid fluid = BuiltInRegistries.FLUID.byId(id);
		return fluid == null ? Fluids.EMPTY : fluid;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
