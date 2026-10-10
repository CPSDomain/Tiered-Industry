package com.robvanblerk.tieredpower.menu;

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

import com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

/** Slots: 0-8 the items to keep (copies), 9+ player. */
public class StockKeeperMenu extends AbstractContainerMenu {
	public static final int PLAYER_START = 9, SLOT_Y = 20;
	private final @Nullable StockKeeperBlockEntity keeper;
	private final Container items;
	private final ContainerData data;

	public StockKeeperMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(9), new SimpleContainerData(27));
	}

	public StockKeeperMenu(int id, Inventory inv, StockKeeperBlockEntity be) {
		this(id, inv, be, be.items(), be.data());
	}

	private StockKeeperMenu(int id, Inventory inv, @Nullable StockKeeperBlockEntity be, Container items, ContainerData data) {
		super(ModMenus.STOCK_KEEPER.get(), id);
		this.keeper = be;
		this.items = items;
		this.data = data;
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(items, i, 8 + i * 18, SLOT_Y) {
				@Override public boolean mayPlace(ItemStack s) { return false; }
				@Override public boolean mayPickup(Player p) { return false; }
			});
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
		addDataSlots(data);
	}

	public int getAmount(int slot) {
		return (data.get(slot * 2) & 0xFFFF) | ((data.get(slot * 2 + 1) & 0xFFFF) << 16);
	}

	public int getStatus(int slot) {
		return data.get(18 + slot);
	}

	/** Server: set how many of slot's item to keep (from StockKeeperPacket). */
	public void setAmount(int slot, int amount) {
		if (keeper == null || slot < 0 || slot >= 9) return;
		if (amount < 0) { // clear the slot
			items.setItem(slot, ItemStack.EMPTY);
			items.setChanged();
			keeper.setAmount(slot, 0);
		} else {
			keeper.setAmount(slot, amount);
		}
	}

	/** The item slots hold copies: click with an item to choose it (keeps 64 by default), empty hand to clear. */
	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId >= 0 && slotId < 9) {
			ItemStack carried = getCarried();
			if (carried.isEmpty()) {
				items.setItem(slotId, ItemStack.EMPTY);
				if (keeper != null) keeper.setAmount(slotId, 0);
			} else {
				items.setItem(slotId, carried.copyWithCount(1));
				if (keeper != null && getAmount(slotId) == 0) keeper.setAmount(slotId, 64);
			}
			items.setChanged();
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < PLAYER_START) return ItemStack.EMPTY;
		ItemStack stack = slots.get(index).getItem();
		if (stack.isEmpty()) return ItemStack.EMPTY;
		for (int i = 0; i < 9; i++) if (ItemStack.isSameItemSameTags(items.getItem(i), stack)) return ItemStack.EMPTY;
		for (int i = 0; i < 9; i++) {
			if (items.getItem(i).isEmpty()) {
				items.setItem(i, stack.copyWithCount(1));
				if (keeper != null) keeper.setAmount(i, 64);
				items.setChanged();
				break;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return slot.index >= PLAYER_START;
	}

	@Override
	public boolean stillValid(Player player) {
		return items.stillValid(player);
	}
}
