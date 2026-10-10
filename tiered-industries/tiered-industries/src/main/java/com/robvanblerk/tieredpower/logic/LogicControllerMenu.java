package com.robvanblerk.tieredpower.logic;

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

/** Slots 0-3: each rule's item (a ghost copy - click with an item to set, empty hand to clear); then the player's. */
public class LogicControllerMenu extends AbstractContainerMenu {
	public static final int ROW_Y = 20, ROW_H = 22, INV_Y = 124;
	private final @Nullable LogicControllerBlockEntity be;
	private final Container items;
	private final ContainerData data;

	public LogicControllerMenu(int id, Inventory inv) {
		this(id, inv, null, new SimpleContainer(LogicControllerBlockEntity.RULES), new SimpleContainerData(LogicControllerBlockEntity.RULES * 8));
	}

	public LogicControllerMenu(int id, Inventory inv, LogicControllerBlockEntity be) {
		this(id, inv, be, be.items, be.data);
	}

	private LogicControllerMenu(int id, Inventory inv, @Nullable LogicControllerBlockEntity be, Container items, ContainerData data) {
		super(ModMenus.LOGIC_CONTROLLER.get(), id);
		this.be = be;
		this.items = items;
		this.data = data;
		for (int r = 0; r < LogicControllerBlockEntity.RULES; r++) {
			addSlot(new Slot(items, r, 8, ROW_Y + r * ROW_H) {
				@Override public boolean mayPlace(ItemStack s) { return false; }
				@Override public boolean mayPickup(Player p) { return false; }
			});
		}
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, INV_Y + row * 18));
		for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, INV_Y + 58));
		addDataSlots(data);
	}

	public int mode(int r) { return data.get(r * 8); }
	public boolean below(int r) { return data.get(r * 8 + 1) == 1; }
	public int side(int r) { return data.get(r * 8 + 2); }
	public int threshold(int r) { return (data.get(r * 8 + 3) & 0xFFFF) | ((data.get(r * 8 + 4) & 0xFFFF) << 16); }
	public int status(int r) { return data.get(r * 8 + 5); }
	public int current(int r) { return (data.get(r * 8 + 6) & 0xFFFF) | ((data.get(r * 8 + 7) & 0xFFFF) << 16); }

	@Override
	public void clicked(int slotId, int button, ClickType type, Player player) {
		if (slotId >= 0 && slotId < LogicControllerBlockEntity.RULES) {
			ItemStack carried = getCarried();
			items.setItem(slotId, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			items.setChanged();
			return;
		}
		super.clicked(slotId, button, type, player);
	}

	/** Buttons: rule * 10 + 0 (mode), 1 (above/below), 2 (side). */
	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (be == null) return false;
		int r = id / 10, what = id % 10;
		if (r < 0 || r >= LogicControllerBlockEntity.RULES) return false;
		switch (what) {
			case 0 -> be.cycleMode(r);
			case 1 -> be.toggleCompare(r);
			case 2 -> be.cycleSide(r);
			default -> { return false; }
		}
		return true;
	}

	/** Server: from LogicValuePacket. */
	public void setThreshold(int r, int v) {
		if (be != null && r >= 0 && r < LogicControllerBlockEntity.RULES) be.setThreshold(r, v);
	}

	@Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
	@Override public boolean stillValid(Player player) { return be == null || !be.isRemoved() && player.distanceToSqr(be.getBlockPos().getCenter()) < 64; }
}
