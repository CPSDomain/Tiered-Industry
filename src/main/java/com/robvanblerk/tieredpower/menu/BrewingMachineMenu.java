package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.BrewingMachineBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class BrewingMachineMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 FE/t, 5 water
	public static final int DATA_COUNT = 6;
	private final Container container;

	public BrewingMachineMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(BrewingMachineBlockEntity.SIZE), new SimpleContainerData(DATA_COUNT));
	}

	public BrewingMachineMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.BREWING_MACHINE.get(), id, data, BrewingMachineBlockEntity.SIZE);
		this.container = container;
		for (int i = 0; i < 3; i++)
			addSlot(new Slot(container, i, 44 + i * 18, 52) {
				@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(getContainerSlot(), s); }
				@Override public int getMaxStackSize() { return 1; }
			});
		addSlot(new Slot(container, BrewingMachineBlockEntity.INGREDIENT, 62, 18) {
			@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(BrewingMachineBlockEntity.INGREDIENT, s); }
		});
		for (int i = 0; i < 3; i++) addOutputSlot(container, BrewingMachineBlockEntity.OUT + i, 116, 16 + i * 18, false);
		addUpgradeSlots(container, BrewingMachineBlockEntity.UPGRADE_SLOT, 4);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return BrewingMachineBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getWater() { return data.get(5); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
