package com.robvanblerk.tieredpower.menu;

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

/** The filter for an Import or Export Bus: 9 "ghost" slots that hold a copy of an item, never the real thing. */
public class StorageBusMenu extends AbstractContainerMenu {
	public static final int FILTER_SLOTS = 9, INV_Y = 102;
	private final Container filter;
	private final ContainerData data;

	public StorageBusMenu(int id, Inventory inv) {
		this(id, inv, new SimpleContainer(FILTER_SLOTS), new SimpleContainerData(2));
	}

	public StorageBusMenu(int id, Inventory inv, Container filter, ContainerData data) {
		super(ModMenus.STORAGE_BUS.get(), id);
		this.filter = filter;
		this.data = data;
		for (int i = 0; i < FILTER_SLOTS; i++) {
			addSlot(new Slot(filter, i, 62 + (i % 3) * 18, 17 + (i / 3) * 18) {
				@Override public boolean mayPlace(ItemStack s) { return false; }
				@Override public boolean mayPickup(Player p) { return false; }
			});
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
		addDataSlots(data);
	}

	public boolean isImport() {
		return data.get(0) == 1;
	}

	public boolean isOnline() {
		return data.get(1) == 1;
	}

	/** Clicking a filter slot copies the held item into it (or clears it with an empty hand). */
	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId >= 0 && slotId < FILTER_SLOTS) {
			ItemStack carried = getCarried();
			filter.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	/** Shift-click in your inventory adds that item to the filter. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < FILTER_SLOTS) return ItemStack.EMPTY;
		ItemStack stack = slots.get(index).getItem();
		if (stack.isEmpty()) return ItemStack.EMPTY;
		for (int i = 0; i < FILTER_SLOTS; i++) if (ItemStack.isSameItem(filter.getItem(i), stack)) return ItemStack.EMPTY;
		for (int i = 0; i < FILTER_SLOTS; i++) {
			if (filter.getItem(i).isEmpty()) {
				filter.setItem(i, stack.copyWithCount(1));
				break;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.container != filter;
	}

	@Override
	public boolean stillValid(Player player) {
		return filter.stillValid(player);
	}
}
