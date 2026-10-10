package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.GasElectrolyzerBlockEntity;
import com.robvanblerk.tieredpower.item.powered.HydrogenJetpackItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class GasElectrolyzerMenu extends MachineMenu {
	// 0-1 energy, 2 water, 3 hydrogen, 4 oxygen, 5 FE/t
	public static final int DATA_COUNT = 6;
	private final Container container;

	public GasElectrolyzerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public GasElectrolyzerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.GAS_ELECTROLYZER.get(), id, data, 3);
		this.container = container;
		addSlot(new Slot(container, 0, 30, 35) {
			@Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof HydrogenJetpackItem; }
			@Override public int getMaxStackSize() { return 1; }
		});
		addUpgradeSlots(container, 1, 5);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return GasElectrolyzerBlockEntity.CAPACITY; }
	public int getWater() { return data.get(2); }
	public int getHydrogen() { return data.get(3); }
	public int getOxygen() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
