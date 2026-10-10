package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModBlocks;
import com.robvanblerk.tieredpower.registry.ModMenus;

public class StormCallerMenu extends MachineMenu {
	// 0-1 energy, 2 status
	public static final int DATA_COUNT = 3;
	private final Container container;

	public StormCallerMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT));
	}

	public StormCallerMenu(int id, Inventory inv, Container container, ContainerData data) {
		super(ModMenus.STORM_CALLER.get(), id, data, 1);
		this.container = container;
		addSlot(new Slot(container, 0, 80, 35) {
			@Override public boolean mayPlace(ItemStack s) { return s.is(ModBlocks.STORM_CHARGE.get()); }
		});
		addPlayerInventory(inv);
	}

	@Override public long getCapacity() { return StormCallerBlockEntity.CAPACITY; }
	public int getStatus() { return data.get(2); }
	@Override public boolean stillValid(Player player) { return container.stillValid(player); }
}
