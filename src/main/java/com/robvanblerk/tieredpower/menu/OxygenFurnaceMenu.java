package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.OxygenFurnaceBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class OxygenFurnaceMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 oxygen, 5 FE/t
	public static final int DATA_COUNT = 6;
	private final Container container;

	public OxygenFurnaceMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(4), new SimpleContainerData(DATA_COUNT));
	}

	public OxygenFurnaceMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.OXYGEN_FURNACE.get(), id, data, 4);
		this.container = container;
		addSlot(new Slot(container, 0, 44, 35) {
			@Override public boolean mayPlace(ItemStack s) { return OxygenFurnaceBlockEntity.isIron(s); }
		});
		addOutputSlot(container, 1, 104, 35, true);
		addUpgradeSlots(container, 2, 5);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return OxygenFurnaceBlockEntity.CAPACITY; }
	public float progress() { return data.get(3) <= 0 ? 0 : (float) data.get(2) / data.get(3); }
	public int getOxygen() { return data.get(4); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
