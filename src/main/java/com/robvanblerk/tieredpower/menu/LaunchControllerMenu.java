package com.robvanblerk.tieredpower.menu;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.LaunchControllerBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Slots: 0 rocket, 1 payload, 2+ player. */
public class LaunchControllerMenu extends AbstractContainerMenu {
	// 0 fuel, 1 structure, 2 countdown seconds (-1 none), 3 satellites in orbit, 4 claimed by dishes, 5 sky clear
	public static final int DATA_COUNT = 6, BUTTON_LAUNCH = 0;
	private final @Nullable LaunchControllerBlockEntity controller;
	private final Container items;
	private final ContainerData data;

	public LaunchControllerMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(2), new SimpleContainerData(DATA_COUNT));
	}

	public LaunchControllerMenu(int id, Inventory inv, @Nullable LaunchControllerBlockEntity be, Container items, ContainerData data) {
		super(ModMenus.LAUNCH_CONTROLLER.get(), id);
		this.controller = be;
		this.items = items;
		this.data = data;
		addSlot(new Slot(items, 0, 26, 30) {
			@Override public boolean mayPlace(ItemStack s) { return items.canPlaceItem(0, s); }
			@Override public int getMaxStackSize() { return 1; }
		});
		addSlot(new Slot(items, 1, 26, 52) {
			@Override public boolean mayPlace(ItemStack s) { return items.canPlaceItem(1, s); }
			@Override public int getMaxStackSize() { return 1; }
		});
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 156));
		addDataSlots(data);
	}

	public int getFuel() { return data.get(0); }
	public int getStructure() { return data.get(1); }
	public int getCountdown() { return data.get(2); }
	public int getSatellites() { return data.get(3); }
	public int getClaimed() { return data.get(4); }
	public boolean isSkyClear() { return data.get(5) == 1; }

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_LAUNCH && controller != null) controller.startLaunch(player);
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < 2) {
			if (!moveItemStackTo(stack, 2, slots.size(), true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override public boolean stillValid(Player player) { return items.stillValid(player); }
}
