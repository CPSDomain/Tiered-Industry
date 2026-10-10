package com.robvanblerk.tieredpower.industry;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.menu.MachineMenu;

/** Coke Oven and Industrial Blast Furnace. */
public class HeavyFurnaceMenu extends MachineMenu {
	// 0 progress, 1 max, 2 creosote
	public static final int DATA_COUNT = 3;
	private final Container container;
	private final boolean coke;

	public HeavyFurnaceMenu(MenuType<HeavyFurnaceMenu> type, int id, Inventory inv, boolean coke) {
		this(type, id, inv, new SimpleContainer(HeavyFurnaceBlockEntity.SIZE), new SimpleContainerData(DATA_COUNT), coke);
	}

	public HeavyFurnaceMenu(MenuType<HeavyFurnaceMenu> type, int id, Inventory inv, Container container, ContainerData data, boolean coke) {
		super(type, id, data, HeavyFurnaceBlockEntity.SIZE);
		this.container = container;
		this.coke = coke;
		addSlot(new Slot(container, 0, 44, coke ? 35 : 26) {
			@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(0, s); }
		});
		addSlot(new Slot(container, 1, 44, 44) {
			@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(1, s); }
			@Override public boolean isActive() { return !HeavyFurnaceMenu.this.coke; }
		});
		addOutputSlot(container, HeavyFurnaceBlockEntity.OUTPUT, 104, 35, true);
		addPlayerInventory(inv);
	}

	public boolean isCokeOven() { return coke; }
	public float progress() { return data.get(1) <= 0 ? 0 : (float) data.get(0) / data.get(1); }
	public int getCreosote() { return data.get(2); }
	@Override public long getCapacity() { return 1; }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
