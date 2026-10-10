package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.ChemicalWasherBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class ChemicalWasherMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 water, 5 chlorine, 6 FE/t
	public static final int DATA_COUNT = 7;
	private final Container container;
	private final Inventory inventory;

	public ChemicalWasherMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public ChemicalWasherMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.CHEMICAL_WASHER.get(), id, data, 4);
		this.container = container;
		this.inventory = inv;
		addSlot(new Slot(container, 0, 30, 35) {
			@Override public boolean mayPlace(ItemStack s) { return !ChemicalWasherBlockEntity.washResult(inventory.player.level(), s).isEmpty(); }
		});
		addOutputSlot(container, 1, 80, 35, true);
		addUpgradeSlots(container, 2, 6);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return ChemicalWasherBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getWater() { return data.get(4); }
	public int getChlorine() { return data.get(5); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
