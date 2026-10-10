package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.item.ItemFilterItem;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Edits the Item Filter held in the main hand: 9 ghost slots plus a Whitelist/Blacklist toggle (button 1). */
public class ItemFilterMenu extends AbstractContainerMenu {
	public static final int GRID_X = 62, GRID_Y = 17;

	private final ItemStack filter; // EMPTY on the client
	private final SimpleContainer pattern = new SimpleContainer(9);
	private final DataSlot whitelist;
	private final int lockedSlot;

	/** Client side. */
	public ItemFilterMenu(int id, Inventory inventory) {
		this(id, inventory, ItemStack.EMPTY);
	}

	public ItemFilterMenu(int id, Inventory inventory, ItemStack filter) {
		super(ModMenus.ITEM_FILTER.get(), id);
		this.filter = filter;
		if (!filter.isEmpty()) {
			var items = ItemFilterItem.getItems(filter);
			for (int i = 0; i < 9 && i < items.size(); i++) {
				if (items.get(i) != null) pattern.setItem(i, new ItemStack(items.get(i)));
			}
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(pattern, i, GRID_X + (i % 3) * 18, GRID_Y + (i / 3) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}

				@Override
				public boolean mayPickup(Player player) {
					return false;
				}
			});
		}
		// Player inventory; the held filter can't be moved while it's open.
		this.lockedSlot = inventory.selected;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
		}
		for (int col = 0; col < 9; col++) {
			final int index = col;
			addSlot(new Slot(inventory, col, 8 + col * 18, 142) {
				@Override
				public boolean mayPickup(Player player) {
					return index != lockedSlot;
				}
			});
		}
		whitelist = new DataSlot() {
			private int clientValue = 1;

			@Override
			public int get() {
				return filter.isEmpty() ? clientValue : (ItemFilterItem.isWhitelist(filter) ? 1 : 0);
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(whitelist);
		mode = new DataSlot() {
			private int clientValue;

			@Override
			public int get() {
				return filter.isEmpty() ? clientValue : ItemFilterItem.getMode(filter);
			}

			@Override
			public void set(int value) {
				clientValue = value;
			}
		};
		addDataSlot(mode);
	}

	private final DataSlot mode;

	public int getMode() {
		return Math.floorMod(mode.get(), ItemFilterItem.MODES.length);
	}

	public boolean isWhitelist() {
		return whitelist.get() != 0;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (slotId >= 0 && slotId < 9) {
			ItemStack carried = getCarried();
			ItemStack ghost = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
			pattern.setItem(slotId, ghost);
			if (!filter.isEmpty()) ItemFilterItem.setItem(filter, slotId, ghost);
			broadcastChanges();
			return;
		}
		if (clickType == ClickType.SWAP && button == lockedSlot) return; // no number-key swapping the held filter
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id == 1 && !filter.isEmpty()) {
			ItemFilterItem.setWhitelist(filter, !ItemFilterItem.isWhitelist(filter));
			return true;
		}
		if (id == 2 && !filter.isEmpty()) {
			ItemFilterItem.setMode(filter, ItemFilterItem.getMode(filter) + 1);
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return filter.isEmpty() || player.getMainHandItem() == filter;
	}
}
