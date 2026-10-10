package com.robvanblerk.tieredpower.industry;

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

public class BioRefineryMenu extends MachineMenu {
	// 0-1 energy, 2 oil, 3 ethanol, 4 biodiesel, 5 progress, 6 max
	public static final int DATA_COUNT = 7;
	private final Container container;

	public BioRefineryMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT));
	}

	public BioRefineryMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.BIO_REFINERY.get(), id, data, 3);
		this.container = container;
		addSlot(new Slot(container, 0, 26, 35) {
			@Override public boolean mayPlace(ItemStack s) { return container.canPlaceItem(0, s); }
		});
		addUpgradeSlots(container, 1, -1);
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return BioRefineryBlockEntity.CAPACITY; }
	public int get(int i) { return data.get(i); }
	public float progress() { return data.get(6) <= 0 ? 0 : (float) data.get(5) / data.get(6); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
