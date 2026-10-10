package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.item.StorageDiskItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class DriveBayMenu extends MachineMenu {
	public static final int DATA_COUNT = 2;
	private final Container container;

	public DriveBayMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(8), new SimpleContainerData(DATA_COUNT));
	}

	public DriveBayMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.DRIVE_BAY.get(), id, data, 8);
		this.container = container;
		for (int i = 0; i < 8; i++) {
			addSlot(new Slot(container, i, 53 + (i % 4) * 18, 26 + (i / 4) * 18) {
				@Override public boolean mayPlace(ItemStack s) { return com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity.isDisk(s); }
				@Override public int getMaxStackSize() { return 1; }
			});
		}
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return 0; }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
