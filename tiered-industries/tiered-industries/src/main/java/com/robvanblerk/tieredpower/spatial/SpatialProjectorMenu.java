package com.robvanblerk.tieredpower.spatial;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.menu.MachineMenu;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class SpatialProjectorMenu extends MachineMenu {
	// 0-1 energy, 2 cube size, 3 cell full
	public static final int DATA_COUNT = 4;
	public static final int CELL_X = 44, CELL_Y = 35;
	private final Container container;

	public SpatialProjectorMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public SpatialProjectorMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.SPATIAL_PROJECTOR.get(), id, data, 1);
		this.container = container;
		addSlot(new Slot(container, 0, CELL_X, CELL_Y) {
			@Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof SpatialCellItem; }
			@Override public int getMaxStackSize() { return 1; }
		});
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return SpatialProjectorBlockEntity.CAPACITY; }
	public int getSize() { return data.get(2); }
	public boolean isCellFull() { return data.get(3) != 0; }

	/** Button 1: capture or deploy. */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 1 && container instanceof SpatialProjectorBlockEntity p) {
			p.activate(player);
			return true;
		}
		return super.clickMenuButton(player, id);
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
