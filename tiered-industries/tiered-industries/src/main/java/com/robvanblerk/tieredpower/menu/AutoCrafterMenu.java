package com.robvanblerk.tieredpower.menu;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.robvanblerk.tieredpower.block.entity.AutoCrafterBlockEntity;
import com.robvanblerk.tieredpower.registry.ModMenus;

/**
 * Auto-Crafter menu. The 3x3 pattern slots are "ghost" slots: clicking one with an item sets a copy of it as the
 * pattern (nothing is taken from you); clicking with an empty hand clears it. They're added after the player
 * inventory so shift-clicking never moves real items into them.
 */
public class AutoCrafterMenu extends MachineMenu {
	// 0-1 energy, 2 progress, 3 max, 4 status, 5 FE/t
	public static final int DATA_COUNT = 6;
	public static final int GRID_X = 30, GRID_Y = 17, OUTPUT_X = 124, OUTPUT_Y = 35;

	private final Container container;
	private final Container pattern;
	private final int ghostStart;

	public AutoCrafterMenu(int containerId, Inventory inventory) {
		this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(DATA_COUNT), new SimpleContainer(9));
	}

	public AutoCrafterMenu(int containerId, Inventory inventory, Container container, ContainerData data, Container pattern) {
		super(ModMenus.AUTO_CRAFTER.get(), containerId, data, 3);
		this.container = container;
		this.pattern = pattern;
		addOutputSlot(container, AutoCrafterBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y, true);
		addUpgradeSlots(container, 1, 5);
		addPlayerInventory(inventory);
		ghostStart = slots.size();
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
	}

	public boolean isGhostSlot(int index) {
		return index >= ghostStart && index < ghostStart + 9;
	}

	public int getGhostStart() {
		return ghostStart;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (isGhostSlot(slotId)) {
			ItemStack carried = getCarried();
			pattern.setItem(slotId - ghostStart, carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
			pattern.setChanged();
			broadcastChanges();
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (isGhostSlot(index)) return ItemStack.EMPTY;
		return super.quickMoveStack(player, index);
	}

	@Override
	public long getCapacity() {
		return AutoCrafterBlockEntity.CAPACITY;
	}

	public float getProgressFraction() {
		int max = data.get(3);
		return max <= 0 ? 0f : (float) data.get(2) / max;
	}

	public int getStatus() { return data.get(4); }

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}
}
