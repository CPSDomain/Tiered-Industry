package com.robvanblerk.tieredpower.quantum;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Quantum Chest window: the 54 chest slots, plus (on a linked chest) a row of 9 ghost filter slots and a Send/Receive
 * button. The filter slots come after the player inventory so shift-clicking never moves items into them.
 */
public class QuantumChestMenu extends AbstractContainerMenu {
	public static final int BUTTON_MODE = 1;
	public static final int FILTER_Y = 18, CHEST_Y = 40, INV_Y = 162, HEIGHT = 244;
	public static final int SLOTS = QuantumChestBlockEntity.SLOTS, FILTER = QuantumChestBlockEntity.FILTER_SLOTS;

	private final Container chest;
	private final Container filter;
	private final ContainerData data;
	private final @Nullable QuantumChestBlockEntity be;
	private final int filterStart;

	/** Client side. */
	public QuantumChestMenu(int id, Inventory inventory) {
		this(id, inventory, null, new SimpleContainer(SLOTS), new SimpleContainer(FILTER), new SimpleContainerData(4));
	}

	public QuantumChestMenu(int id, Inventory inventory, QuantumChestBlockEntity be) {
		this(id, inventory, be, be, be.getFilter(), be.getData());
	}

	private QuantumChestMenu(int id, Inventory inventory, @Nullable QuantumChestBlockEntity be, Container chest, Container filter, ContainerData data) {
		super(ModMenus.QUANTUM_CHEST.get(), id);
		this.be = be;
		this.chest = chest;
		this.filter = filter;
		this.data = data;
		chest.startOpen(inventory.player);
		for (int row = 0; row < 6; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(chest, col + row * 9, 8 + col * 18, CHEST_Y + row * 18));
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, INV_Y + 58));
		filterStart = slots.size();
		for (int i = 0; i < FILTER; i++) {
			addSlot(new Slot(filter, i, 8 + i * 18, FILTER_Y) {
				@Override public boolean mayPlace(ItemStack stack) { return false; }
				@Override public boolean mayPickup(Player player) { return false; }
				@Override public boolean isActive() { return !isMain(); }
			});
		}
		addDataSlots(data);
	}

	public boolean isMain() { return data.get(0) != 0; }
	public boolean isReceive() { return data.get(1) != 0; }
	public int sending() { return data.get(2); }
	public int receiving() { return data.get(3); }

	public boolean filterEmpty() {
		for (int i = 0; i < FILTER; i++) if (!filter.getItem(i).isEmpty()) return false;
		return true;
	}

	private boolean isFilterSlot(int index) {
		return index >= filterStart && index < filterStart + FILTER;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (isFilterSlot(slotId)) {
			if (!isMain()) {
				ItemStack carried = getCarried();
				filter.setItem(slotId - filterStart, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
				broadcastChanges();
			}
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == BUTTON_MODE && be != null && !be.isMain()) {
			be.toggleMode();
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index >= filterStart) return ItemStack.EMPTY;
		Slot slot = slots.get(index);
		if (!slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < SLOTS) {
			if (!moveItemStackTo(stack, SLOTS, filterStart, true)) return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
		else slot.setChanged();
		return copy;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		chest.stopOpen(player);
	}

	@Override
	public boolean stillValid(Player player) {
		return chest.stillValid(player);
	}
}
